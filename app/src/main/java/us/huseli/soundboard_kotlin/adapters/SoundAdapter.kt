package us.huseli.soundboard_kotlin.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.SoundItemLongClickAnimator
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModelFactory


class SoundAdapter(private val viewModelStoreOwner: ViewModelStoreOwner, private val appViewModel: AppViewModel) :
        DataBoundAdapter<Sound, SoundAdapter.ViewHolder, ItemSoundBinding>(),
        ItemDragHelperAdapter<Sound> {

    private var onItemsReorderedCallback: ((sounds: List<Sound>) -> Unit)? = null
    override val currentList = mutableListOf<Sound>()

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: Sound) {
        val viewModelFactory = SoundViewModelFactory(item)
        val viewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory).get(item.id.toString(), SoundViewModel::class.java)
        holder.bind(viewModel)
    }

    override fun onItemsReordered() = onItemsReorderedCallback?.invoke(currentList)

    override fun calculateDiff(list: List<Sound>) = DiffUtil.calculateDiff(DiffCallback(list, currentList), true).dispatchUpdatesTo(this)

    fun setOnItemsReordered(function: (sounds: List<Sound>) -> Unit) {
        onItemsReorderedCallback = function
    }


    inner class DiffCallback(newRows: List<Sound>, oldRows: List<Sound>) :
            DataBoundAdapter<Sound, SoundAdapter.ViewHolder, ItemSoundBinding>.DiffCallback(newRows, oldRows) {
        override fun areItemsTheSame(oldItem: Sound, newItem: Sound) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Sound, newItem: Sound) = oldItem.name == newItem.name && oldItem.uri == newItem.uri
    }


    inner class ViewHolder(binding: ItemSoundBinding, private val context: Context) :
            DataBoundViewHolder<ItemSoundBinding>(binding),
            View.OnClickListener,
            View.OnLongClickListener,
            AppViewModelListenerInterface {
        private val clickAnimator = (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }
        private var categoryId: Int? = null
        private var selectEnabled = false
        private var reorderEnabled = false
        private lateinit var longClickAnimator: SoundItemLongClickAnimator
        private lateinit var viewModel: SoundViewModel
        private var sound: Sound? = null
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundCard.setOnClickListener(this)
            binding.soundCard.setOnLongClickListener(this)
        }

        fun bind(viewModel: SoundViewModel) {
            Log.i(GlobalApplication.LOG_TAG,
                    "SoundAdapter.ViewHolder.bind: adapter ${this@SoundAdapter.hashCode()}, " +
                            "viewHolder ${hashCode()}, SoundViewModel $viewModel")

            this.viewModel = viewModel
            binding.viewModel = viewModel

            if (!viewModel.isValid)
                binding.failIcon.visibility = View.VISIBLE

            viewModel.sound.observe(this, { this.sound = it })

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                viewModel.backgroundColor.observe(this, { color ->
                    longClickAnimator = SoundItemLongClickAnimator(binding.soundCard, color)
                    binding.volumeBar.progressDrawable.alpha = 150
                    binding.volumeBar.progressTintMode = PorterDuff.Mode.OVERLAY
                    binding.volumeBar.progressTintList =
                            if (GlobalApplication.colorHelper.getLuminance(color) >= 0.6) ColorStateList.valueOf(Color.BLACK)
                            else ColorStateList.valueOf(Color.WHITE)
                })
            }

            viewModel.isPlaying.observe(this, { binding.playIcon.visibility = if (it) View.VISIBLE else View.INVISIBLE })
            viewModel.isSelected.observe(this, { onIsSelectedChange(it) })
            viewModel.categoryId.observe(this, { categoryId = it })
            appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
            appViewModel.selectEnabled.observe(this, { onSelectEnabledChange(it) })
        }

        override fun onSelectEnabledChange(value: Boolean) {
            selectEnabled = value
            if (!value)
                viewModel.unselect()
        }

        private fun onIsSelectedChange(value: Boolean) {
            try {
                if (value) {
                    binding.selectedIcon.visibility = View.VISIBLE
                    appViewModel.selectSound(sound!!)
                } else {
                    binding.selectedIcon.visibility = View.INVISIBLE
                    appViewModel.deselectSound(sound!!)
                }
            } catch (e: NullPointerException) {
            }
        }

        override fun onReorderEnabledChange(value: Boolean) {
            reorderEnabled = value
        }

        override fun onLongClick(v: View): Boolean {
            longClickAnimator.start()
            return if (reorderEnabled) false
            else {
                viewModel.select()
                true
            }
        }

        override fun onClick(view: View?) {
            if (selectEnabled) viewModel.toggleSelected()
            else if (!viewModel.isValid) showErrorToast()
            else viewModel.playOrPause()
            clickAnimator.start()
        }

        override fun toString() = super.toString() + " '" + binding.soundName.text + "'"

        private fun showErrorToast() = Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()
    }
}