package us.huseli.soundboard_kotlin.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.SoundItemLongClickAnimator
import us.huseli.soundboard_kotlin.data.AbstractSound
import us.huseli.soundboard_kotlin.data.DraggedSound
import us.huseli.soundboard_kotlin.data.EmptySound
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ItemEmptySoundBinding
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.viewmodels.*
import java.util.*


class SoundAdapter(
        private val viewModelStoreOwner: ViewModelStoreOwner,
        private val appViewModel: AppViewModel,
        private val categoryViewModel: CategoryViewModel,
        private val recyclerView: RecyclerView) :
        //SoundDragCallback2,
        DataBoundAdapter<AbstractSound, SoundAdapter.AbstractViewHolder, ViewDataBinding>(DiffCallback()) {

    private var emptySound: EmptySound? = null
    private var emptySoundViewModel: EmptySoundViewModel? = null
    private var draggedView: View? = null
    //private val emptySound = EmptySound()
    //val soundDragListener = SoundDragListener2(this)

    init {
        setHasStableIds(true)
        registerAdapterDataObserver(DataObserver())
    }

    class DataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            Log.d(LOG_TAG, "onChanged")
            super.onChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeChanged<positionStart=$positionStart, itemCount=$itemCount>")
            super.onItemRangeChanged(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            Log.d(LOG_TAG, "onItemRangeChanged<positionStart=$positionStart, itemCount=$itemCount, payload=$payload>")
            super.onItemRangeChanged(positionStart, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeInserted<positionStart=$positionStart, itemCount=$itemCount>")
            super.onItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeRemoved<positionStart=$positionStart, itemCount=$itemCount>")
            super.onItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeMoved<fromPosition=$fromPosition, toPosition=$toPosition, itemCount=$itemCount>")
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
        }

        companion object {
            const val LOG_TAG = "DataObserver"
        }
    }

    fun createEmptySound(categoryId: Int) {
        // Used by CategoryAdapter.ViewHolder.bind()
        if (emptySound == null) {
            //emptySound = EmptySound(categoryId, currentList.size)
            emptySound = EmptySound(categoryId)
        }
    }

    /**
     * Various implemented/overridden methods
     */
    override fun bind(holder: AbstractViewHolder, item: AbstractSound, position: Int) {
        if (holder is SoundViewHolder && item is Sound) {
            val viewModelFactory = SoundViewModelFactory(item)
            val viewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory).get(item.id.toString(), SoundViewModel::class.java)
            holder.bind(viewModel)
        } else if (holder is EmptySoundViewHolder && item is EmptySound) {
            val viewModelFactory = EmptySoundViewModelFactory(item, position)
            emptySoundViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory).get(item.categoryId.toString(), EmptySoundViewModel::class.java)
        }
    }

    override fun createBinding(parent: ViewGroup, viewType: Int): ViewDataBinding {
        return when (viewType) {
            SOUND_VIEW_TYPE -> ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            EMPTY_SOUND_VIEW_TYPE -> ItemEmptySoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> throw RuntimeException("Invalid viewType: $viewType")
        }
    }

    override fun createViewHolder(binding: ViewDataBinding, parent: ViewGroup): AbstractViewHolder {
        //if (parent is RecyclerView) recyclerView = parent
        return when (binding) {
            is ItemSoundBinding -> SoundViewHolder(binding, parent.context)
            is ItemEmptySoundBinding -> EmptySoundViewHolder(binding)
            else -> throw RuntimeException("Invalid binding type: $binding")
        }
    }

    override fun getItemById(id: Int) = currentList.find { it.id == id }

    override fun getItemId(position: Int): Long {
        try {
            return currentList[position].id!!.toLong()
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Sound at $position (${currentList[position]}) has null id")
            throw e
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is Sound -> SOUND_VIEW_TYPE
            is EmptySound -> EMPTY_SOUND_VIEW_TYPE
            else -> 0
        }
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundAdapter $hashCode <currentList=$currentList>"
    }

    /**
     * Implemented SoundDragCallback2 methods
     */
    /*
    override fun addEmptySoundIfNecessary() {
        emptySound?.let {
            if (!currentList.contains(it)) {
                Log.d(LOG_TAG, "addEmptySoundIfNecessary: adding $it last")
                addItem(it)
            }
        }
    }
     */

    fun cancelDrop() {
        draggedView?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    cancelDragAndDrop()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, e.toString())
                }
            }
            cancelPendingInputEvents()
        }
    }

    fun collapseCategory() = categoryViewModel.collapse()

    fun containsSound(sound: Sound) = currentList.indexOf(sound) > -1

    fun expandCategory() = categoryViewModel.expand()

    fun getSoundViewHolderUnder(x: Float, y: Float): SoundViewHolder? {
        recyclerView.findChildViewUnder(x, y)?.let { view ->
            recyclerView.findContainingViewHolder(view)?.let { viewHolder ->
                if (viewHolder is SoundViewHolder) return viewHolder
            }
        }
        return null
    }

    fun hideSound(sound: Sound) {
        val position = currentList.indexOf(sound)
        recyclerView.getChildAt(position)?.let { view ->
            view.visibility = View.INVISIBLE
        }
        //removeItemById(soundId)

        //currentList.find { it.id == soundId }?.let { removeItem(it) }
    }

    /**
     * The different insertOrMoveSound() variants should _not_ update DB, only run submitList()
     * with an updated Sound list. DB should be updated when we know the drag operation is well
     * and truly finished.
     * TODO: How do we know this?
     */
    fun insertOrMoveSound(sound: Sound, toPosition: Int) {
        // Remove sound from old position if it already is in list
        val fromPosition = currentList.indexOf(sound)
        val sounds = currentList.toMutableList()

        when {
            toPosition == -1 -> sounds.add(sound)
            fromPosition == -1 -> sounds.add(toPosition, sound)
            fromPosition < toPosition -> for (i in fromPosition until toPosition) Collections.swap(sounds, i, i + 1)
            else -> for (i in fromPosition downTo toPosition + 1) Collections.swap(sounds, i, i - 1)
        }
        submitList(sounds)
    }

    fun markSoundsForDrop(viewHolder: SoundViewHolder) {
        viewHolder.binding.dropMarkerBefore.visibility = View.VISIBLE
        if (viewHolder.adapterPosition > 0) {
            // If there is other viewholder before this one, make its right margin red
            (recyclerView.findViewHolderForAdapterPosition(viewHolder.adapterPosition - 1) as? SoundViewHolder)?.apply {
                binding.dropMarkerAfter.visibility = View.VISIBLE
            }
        }
    }

    fun isEmpty() = currentList.isEmpty()

    fun removeMarksForDrop() {
        viewHolders.filterIsInstance<SoundViewHolder>().forEach {
            it.binding.dropMarkerAfter.visibility = View.INVISIBLE
            it.binding.dropMarkerBefore.visibility = View.INVISIBLE
        }
    }

/*
    override fun moveEmptySound(toPosition: Int) {
        /**
         * Called by SoundDragListener2.
         * We already know that toPosition does not contain emptySound or the dragged Sound.
         */
        if (currentList.contains(emptySound)) {
            val fromPosition = currentList.indexOf(emptySound)
            Log.i(LOG_TAG, "moveEmptySound: moving $emptySound from $fromPosition to $toPosition")
            moveItem(fromPosition, toPosition)
        } else {
            Log.e(LOG_TAG, "moveEmptySound: $emptySound not in $currentList!")
        }
    }
     */

    /*
    override fun removeEmptySound() {
        emptySound?.let { removeItem(it) }
        //emptySoundViewModel?.hide()
    }
     */

    // override fun removeSound(sound: Sound) = removeItem(sound)

    fun showSound(sound: Sound) {
        val position = currentList.indexOf(sound)
        recyclerView.getChildAt(position)?.let { view ->
            view.visibility = View.VISIBLE
        }
    }

    fun updateDb() {
        val sounds = currentList.filterIsInstance<Sound>()
        Log.d(LOG_TAG, "updateDb with $sounds")
        categoryViewModel.updateSounds(sounds)
    }


    companion object {
        const val SOUND_VIEW_TYPE = 1
        const val EMPTY_SOUND_VIEW_TYPE = 2
    }


    class DiffCallback : DiffUtil.ItemCallback<AbstractSound>() {
        override fun areItemsTheSame(oldItem: AbstractSound, newItem: AbstractSound): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AbstractSound, newItem: AbstractSound): Boolean {
            return oldItem.name == newItem.name && oldItem.uri == newItem.uri && oldItem.categoryId == newItem.categoryId && oldItem.order == newItem.order
        }
    }


    abstract inner class AbstractViewHolder(binding: ViewDataBinding) : DataBoundViewHolder<ViewDataBinding, AbstractSound>(binding)


    inner class EmptySoundViewHolder(override val binding: ItemEmptySoundBinding) : AbstractViewHolder(binding) {
        override val lifecycleRegistry = LifecycleRegistry(this)
    }


    @SuppressLint("ClickableViewAccessibility")
    inner class SoundViewHolder(override val binding: ItemSoundBinding, private val context: Context) :
    //DataBoundViewHolder<ItemSoundBinding, AbstractSound>(binding),
            AbstractViewHolder(binding),
            View.OnClickListener,
            View.OnLongClickListener,
            View.OnTouchListener {
        var viewModel: AbstractSoundViewModel? = null

        private val clickAnimator = (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }

        //private val originalBackgroundDrawable = binding.soundCard.background.current
        private var selectEnabled = false
        private var reorderEnabled = false
        private var isValid = true

        //private var draggedSound: DraggedSound? = null
        private var longClickAnimator: SoundItemLongClickAnimator? = null

        //var sound: AbstractSound? = null
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundContainer.setOnClickListener(this)
            binding.soundContainer.setOnLongClickListener(this)
            binding.soundContainer.setOnTouchListener(this)
        }

        fun bind(viewModel: SoundViewModel) {
            Log.i(LOG_TAG, "ViewHolder.bind: adapter=${this@SoundAdapter}, viewHolder=$this, SoundViewModel=$viewModel")

            this.viewModel = viewModel
            binding.viewModel = viewModel

            //viewModel.sound.observe(this) { this.sound = it }

            viewModel.isValid.observe(this) {
                if (!it) binding.failIcon.visibility = View.VISIBLE
                else binding.failIcon.visibility = View.INVISIBLE
                isValid = it
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                viewModel.backgroundColor.observe(this) { color ->
                    longClickAnimator = SoundItemLongClickAnimator(binding.soundCard, color)
                    binding.volumeBar.progressDrawable.alpha = 150
                    binding.volumeBar.progressTintMode = PorterDuff.Mode.OVERLAY
                    binding.volumeBar.progressTintList =
                            if (GlobalApplication.colorHelper.getLuminance(color) >= 0.6) ColorStateList.valueOf(Color.BLACK)
                            else ColorStateList.valueOf(Color.WHITE)
                }
            }

            viewModel.isPlaying.observe(this) { binding.playIcon.visibility = if (it) View.VISIBLE else View.INVISIBLE }
            viewModel.isSelected.observe(this) { onIsSelectedChange(it) }
/*
                viewModel.isDragged.observe(this) {
                    Log.i(LOG_TAG, "viewHolder=$this, isDragged=$it")
                    binding.soundCard.background =
                            if (it) ResourcesCompat.getDrawable(binding.soundCard.resources, R.drawable.border, null)
                            else originalBackgroundDrawable
                }
*/

            appViewModel.reorderEnabled.observe(this) { value -> onReorderEnabledChange(value) }
            appViewModel.selectEnabled.observe(this) { onSelectEnabledChange(viewModel, it) }
        }

        private fun onSelectEnabledChange(viewModel: SoundViewModel, value: Boolean) {
            selectEnabled = value
            if (!value)
                viewModel.unselect()
        }

        private fun onIsSelectedChange(value: Boolean) {
            (viewModel as? SoundViewModel)?.let {
                try {
                    if (value) {
                        // Will enable select and also disable reorder:
                        appViewModel.selectSound(it.sound)
                        binding.selectedIcon.visibility = View.VISIBLE
                    } else {
                        // Will also disable select if this was the last selected sound
                        appViewModel.deselectSound(it.sound)
                        binding.selectedIcon.visibility = View.INVISIBLE
                    }
                } catch (e: NullPointerException) {
                }
            }
        }

        private fun onReorderEnabledChange(value: Boolean) {
            reorderEnabled = value
            binding.reorderIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (reorderEnabled) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    /*
                    if (draggedSound == null || draggedSound?.isDragged == false) {
                        startDragAndDrop(view)
                        return true
                    }

                     */
                    Log.d(LOG_TAG, "onTouch: run startDragAndDrop on $view")
                    startDragAndDrop(view)
                }
                return false
            }
            return view.onTouchEvent(event)
        }

        override fun onLongClick(v: View): Boolean {
            if (!reorderEnabled) {
                (viewModel as? SoundViewModel)?.let {
                    longClickAnimator?.start()
                    it.select()
                }
            }
            return true
        }

        override fun onClick(view: View) {
            (viewModel as? SoundViewModel)?.let {
                if (selectEnabled) it.toggleSelected()
                else if (!isValid) showErrorToast()
                else it.playOrPause()
                clickAnimator.start()
            }
        }

        private fun startDragAndDrop(view: View) {
            (viewModel as? SoundViewModel)?.sound?.let { sound ->
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(view)
                val draggedSound = DraggedSound(sound, this@SoundAdapter, adapterPosition)

                Log.d(LOG_TAG, "startDragAndDrop: draggedSound=$draggedSound, this=$this")

                draggedView = view

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val retval = view.startDragAndDrop(data, shadowBuilder, draggedSound, 0)
                    @Suppress("ControlFlowWithEmptyBody")
                    if (!retval)
                        Log.e(LOG_TAG, "startDragAndDrop: view.startDragAndDrop() returned false")
                    else {
                    }
                } else
                    view.startDrag(data, shadowBuilder, draggedSound, 0)
            }
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "SoundAdapter.ViewHolder $hashCode <adapterPosition=$adapterPosition, sound=${viewModel?.sound}>"
        }

        private fun showErrorToast() = Toast.makeText(context, viewModel?.errorMessage, Toast.LENGTH_SHORT).show()

    }
}