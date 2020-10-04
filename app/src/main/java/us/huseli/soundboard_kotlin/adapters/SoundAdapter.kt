package us.huseli.soundboard_kotlin.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.SoundItemLongClickAnimator
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.helpers.SoundDragListener
import us.huseli.soundboard_kotlin.helpers.SoundItemDragHelperCallback
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModelFactory


class SoundAdapter(
        private val viewModelStoreOwner: ViewModelStoreOwner,
        private val appViewModel: AppViewModel,
        private val categoryViewModel: CategoryViewModel,
        private val soundDragListener: SoundDragListener) :
        DataBoundAdapter<Sound, SoundAdapter.ViewHolder, ItemSoundBinding>() {

    val soundItemTouchHelper = ItemTouchHelper(SoundItemDragHelperCallback())
    override val currentList = mutableListOf<Sound>()

    override fun toString() = "<SoundAdapter ${this.hashCode()} currentList=$currentList>"

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: Sound) {
        val viewModelFactory = SoundViewModelFactory(item)
        val viewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory).get(item.id.toString(), SoundViewModel::class.java)
        holder.bind(viewModel)
    }

    override fun calculateDiff(list: List<Sound>) = DiffUtil.calculateDiff(DiffCallback(list, currentList), true).dispatchUpdatesTo(this)

    fun updateDb() = categoryViewModel.updateSounds(currentList)


    companion object {
        const val LOG_TAG = "SoundAdapter"
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
            View.OnDragListener,
            View.OnTouchListener,
            AppViewModelListenerInterface {
        private val clickAnimator = (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }
        private var categoryId: Int? = null
        private var selectEnabled = false
        private var reorderEnabled = false
        private lateinit var longClickAnimator: SoundItemLongClickAnimator
        private lateinit var viewModel: SoundViewModel
        var sound: Sound? = null
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundContainer.setOnClickListener(this)
            binding.soundContainer.setOnLongClickListener(this)
            binding.soundContainer.setOnDragListener(this)
            binding.soundContainer.setOnTouchListener(this)
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
                    // Will enable select and also disable reorder:
                    appViewModel.selectSound(sound!!)
                    binding.selectedIcon.visibility = View.VISIBLE
                } else {
                    // Will also disable select if this was the last selected sound
                    appViewModel.deselectSound(sound!!)
                    binding.selectedIcon.visibility = View.INVISIBLE
                }
            } catch (e: NullPointerException) {
            }
        }

        override fun onReorderEnabledChange(value: Boolean) {
            reorderEnabled = value
            binding.reorderIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN && reorderEnabled) {
                startDragAndDrop(view)
                return false
            }
            return view.onTouchEvent(event)
        }

        override fun onLongClick(v: View): Boolean {
            if (!reorderEnabled) {
                longClickAnimator.start()
                viewModel.select()
            }
            return true
        }

        override fun onClick(view: View) {
            if (reorderEnabled) startDragAndDrop(view)
            else if (selectEnabled) viewModel.toggleSelected()
            else if (!viewModel.isValid) showErrorToast()
            else viewModel.playOrPause()
            clickAnimator.start()
        }

        private fun startDragAndDrop(view: View) {
            view.tag = adapterPosition
            val data = ClipData.newPlainText("", "")
            val shadowBuilder = View.DragShadowBuilder(view)

            soundDragListener.start(this@SoundAdapter, adapterPosition, viewModel)

            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                view.startDragAndDrop(data, shadowBuilder, view, View.DRAG_FLAG_OPAQUE)
            else
                view.startDrag(data, shadowBuilder, view, 0)
        }

        override fun toString() = super.toString() + " '" + binding.soundName.text + "'"

        /**
         * This method is called on the _receiving_ item, i.e. the one that's being dragged over.
         * Not the dragged one!
         */
        override fun onDrag(v: View, event: DragEvent) = soundDragListener.onDrag(v, event, viewModel, adapterPosition)

        private fun showErrorToast() = Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()

    }
}