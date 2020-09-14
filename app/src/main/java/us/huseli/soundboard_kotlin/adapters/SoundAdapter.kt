package us.huseli.soundboard_kotlin.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.SoundItemLongClickAnimator
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.fragments.CategoryListFragment
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModelFactory


class SoundAdapter(val fragment: CategoryListFragment) :
        DataBoundAdapter<Sound, SoundAdapter.ViewHolder, ItemSoundBinding>(),
        ItemDragHelperAdapter<Sound> {

    private val activity by lazy { fragment.requireActivity() as EditSoundInterface }
    private var onItemsReorderedCallback: ((sounds: List<Sound>) -> Unit)? = null
    override val currentList = mutableListOf<Sound>()

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: Sound) = holder.bind(item)

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
            PopupMenu.OnMenuItemClickListener,
            AppViewModelListenerInterface {
        private val clickAnimator = (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply { setTarget(binding.soundCard) }
        private val colorHelper = ColorHelper(context)
        private lateinit var longClickAnimator: SoundItemLongClickAnimator
        private lateinit var viewModel: SoundViewModel
        private var categoryId: Int? = null
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundCard.setOnClickListener(this)
            binding.soundCard.setOnLongClickListener(this)
        }

        fun bind(item: Sound) {
            Log.i(GlobalApplication.LOG_TAG,
                    "SoundAdapter.ViewHolder.bind: adapter ${this@SoundAdapter.hashCode()}, " +
                            "viewHolder ${hashCode()}, SoundViewModel $item")

            val viewModelFactory = SoundViewModelFactory(item)
            viewModel = ViewModelProvider(fragment, viewModelFactory).get(item.id.toString(), SoundViewModel::class.java)

            binding.viewModel = viewModel

            if (!viewModel.isValid)
                binding.failIcon.visibility = View.VISIBLE

            viewModel.backgroundColor.observe(this, { color ->
                longClickAnimator = SoundItemLongClickAnimator(binding.soundCard, color)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    if (colorHelper.getLuminance(color) >= 0.6)
                        binding.volumeBar.progressDrawable.alpha = 255
                    else
                        binding.volumeBar.progressDrawable.alpha = 127
                    binding.volumeBar.progressDrawable.colorFilter = BlendModeColorFilter(color, BlendMode.HUE)
                }
            })
            viewModel.isPlaying.observe(this, { onIsPlayingChange(it) })
            viewModel.categoryId.observe(this, { categoryId = it })
            fragment.appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
        }

        private fun onIsPlayingChange(value: Boolean) {
            binding.playIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun onReorderEnabledChange(value: Boolean) {
            if (value) binding.soundCard.setOnLongClickListener(null) else binding.soundCard.setOnLongClickListener(this)
        }

        override fun onZoomLevelChange(value: Int) = Unit

        override fun onLongClick(v: View?): Boolean {
            v?.let {
                PopupMenu(v.context, v).apply {
                    menuInflater.inflate(R.menu.sound_context_menu, menu)
                    setOnMenuItemClickListener(this@ViewHolder)
                    show()
                }
                longClickAnimator.start()
            }
            return true
        }

        override fun onClick(view: View?) {
            if (!viewModel.isValid) showErrorToast() else viewModel.playOrPause()
            clickAnimator.start()
        }

        fun onItemSelected() = longClickAnimator.start()

        override fun toString() = super.toString() + " '" + binding.soundName.text + "'"

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            try {
                when (item?.itemId) {
                    R.id.sound_context_menu_edit ->
                        activity.showSoundEditDialog(viewModel.id!!, categoryId)
                    R.id.sound_context_menu_delete ->
                        activity.showSoundDeleteDialog(viewModel.id!!, viewModel.name.value)
                }
            } catch (e: NullPointerException) {
                Toast.makeText(context, R.string.data_not_fetched_yet, Toast.LENGTH_SHORT).show()
            }
            return true
        }

        private fun showErrorToast() = Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()
    }
}