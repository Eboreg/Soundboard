package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.sentry.core.Sentry
import us.huseli.soundboard_kotlin.adapters.SoundAdapter
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

/**
 * To be instantiated once, by an object above SoundAdapter.
 *
 * When drag is started: Store dragged item and its original adapter and position
 * When another item is being dragged over: Update dragged item with that item's adapter and
 * position
 * When item is dropped: Just update DB
 * When drag is cancelled: Revert to original adapter and position
 * We know that a drag has been cancelled if there is an ACTION_DRAG_ENDED event which has _not_
 * been preceeded by an ACTION_DROP event.
 *
 * Desired graphic effects:
 * When item enters another item's place (includes its own, because ACTION_DRAG_ENTERED is fired
 * even before it's moved anywhere), hide that other item but don't remove it from layout
 * When item moves to another category, adapt that colour
 */
class SoundDragListener {
    var isDragging = false

    var soundViewModel: SoundViewModel? = null
    var originalAdapter: SoundAdapter? = null
    var originalPosition: Int? = null
    var currentAdapter: SoundAdapter? = null
    var currentPosition: Int? = null

    private fun reset() {
        isDragging = false
        soundViewModel = null
        originalAdapter = null
        originalPosition = null
        currentAdapter = null
        currentPosition = null
    }

    fun start(adapter: SoundAdapter, position: Int, viewModel: SoundViewModel) {
        Log.i(LOG_TAG, "Start drag: adapter $adapter, position $position, viewModel $viewModel")
        soundViewModel = viewModel
        originalAdapter = adapter
        originalPosition = position
        currentAdapter = adapter
        currentPosition = position
        isDragging = true
    }

    // Returns true if any actual move took place, false otherwise
    private fun move(sound: Sound, fromAdapter: SoundAdapter, toAdapter: SoundAdapter, fromPosition: Int, toPosition: Int): Boolean {
        if (fromAdapter !== toAdapter || fromPosition != toPosition) {
            Log.i(LOG_TAG, "move(): Move $sound from $fromAdapter, position $fromPosition to $toAdapter, position $toPosition")
            fromAdapter.currentList.removeAt(fromPosition)
            toAdapter.currentList.add(toPosition, sound)
            if (fromAdapter === toAdapter) {
                Log.i(LOG_TAG, "move(): Run notifyItemMoved($fromPosition, $toPosition) on $fromAdapter")
                fromAdapter.notifyItemMoved(fromPosition, toPosition)
            } else {
                Log.i(LOG_TAG, "move(): Run notifyItemRemoved($fromPosition) on $fromAdapter")
                fromAdapter.notifyItemRemoved(fromPosition)
                Log.i(LOG_TAG, "move(): Run notifyItemInserted($toPosition) on $toAdapter")
                toAdapter.notifyItemInserted(toPosition)
            }
            return true
        }
        return false
    }

    private fun updateDb() {
        Log.i(LOG_TAG, "updateDb(): Running updateDb() on currentAdapter=$currentAdapter")
        currentAdapter?.updateDb()
        if (currentAdapter !== originalAdapter) {
            Log.i(LOG_TAG, "updateDb(): Running updateDb() on originalAdapter=$originalAdapter")
            originalAdapter?.updateDb()
        }
    }

    fun onDrag(targetView: View, event: DragEvent, viewModel: SoundViewModel, targetPosition: Int): Boolean {
        // targetView = target RecyclerView
        try {
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    try {
                        val list = targetView.parent as RecyclerView
                        val adapter = list.adapter as SoundAdapter

                        Log.i(LOG_TAG, "ACTION_DRAG_ENTERED: $soundViewModel entered view for viewModel=$viewModel, adapter=$adapter")
                        Log.i(LOG_TAG, "ACTION_DRAG_ENTERED: Making $targetView ($viewModel) invisible")

                        targetView.visibility = View.INVISIBLE

                        if (move(soundViewModel!!.sound.value!!, currentAdapter!!, adapter, currentPosition!!, targetPosition)) {
                            currentAdapter = adapter
                            currentPosition = targetPosition
                            soundViewModel!!.setCategoryId(viewModel.categoryId.value!!)
                        }
                    } catch (e: TypeCastException) {
                        Sentry.captureException(e)
                        Log.e(LOG_TAG, "Drag entered: type cast error", e)
                    }
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    Log.i(LOG_TAG, "ACTION_DRAG_EXITED: $soundViewModel exited $targetView ($viewModel)")
                    // Don't make visible if it's the dragged item itself
                    if (viewModel !== soundViewModel) {
                        Log.i(LOG_TAG, "ACTION_DRAG_EXITED: Making $targetView ($viewModel) visible")
                        targetView.visibility = View.VISIBLE
                    }
                }
                DragEvent.ACTION_DROP -> {
                    Log.i(LOG_TAG, "ACTION_DROP: currentAdapter $currentAdapter, currentPosition $currentPosition, soundViewModel $soundViewModel, target viewModel $viewModel, targetView $targetView, targetPosition $targetPosition")
                    Log.i(LOG_TAG, "ACTION_DROP: Making $targetView ($viewModel) visible")
                    targetView.visibility = View.VISIBLE
                    updateDb()
                    reset()
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    Log.i(LOG_TAG, "ACTION_DRAG_ENDED: currentAdapter $currentAdapter, currentPosition $currentPosition, soundViewModel $soundViewModel, target viewModel $viewModel, targetView $targetView, targetPosition $targetPosition")
                    targetView.visibility = View.VISIBLE
                    // Try preserving the state we were at last
                    updateDb()
                    reset()
/*
                    if (isDragging) {  // ACTION_DROP has not occurred
                        move(soundViewModel!!.sound.value!!, currentAdapter!!, originalAdapter!!, currentPosition!!, originalPosition!!)
                        reset()
                    }
*/
                }
            }
        } catch (e: NullPointerException) {
            reset()
            Sentry.captureException(e)
            Log.e(LOG_TAG, "Null pointer exception", e)
        }
        return true
    }

    private fun onDrop(data: DataHolder) {
        data.sourceAdapter.updateDb()
        if (data.sourceAdapter !== data.targetAdapter) data.targetAdapter.updateDb()
    }

    private fun onEntered(data: DataHolder) {
        /**
         * What I need to know here, that I don't know now:
         * 1. What was the item's _last_ adapter? (Not the original one)
         * 2. What was the item's _last_ position? (Not the original one)
         * How will I know that?
         * Not by saving it as properties in this object, since it's tied to the target, not to the
         * dragged item.
         * I should have one single object that keeps track of all draggers _and_ draggees, i.e.
         * all SoundAdapters.
         * When a drag is started, a method runs on it and it saves the dragged item's adapter and
         * position.
         * When another item is dragged over, another method on the same object runs, updating
         * both items' adapters with their new positions. At the same time, it updates its own
         * 'dragged item adapter and position' data.
         * Original adapter and position should also be preserved, in case the drag is cancelled.
         * Where will this object be instantiated?
         * It must be at a level above SoundAdapter, since there are several of those.
         * Also above CategoryAdapter.ViewHolder, since is only holds one SoundAdapter.
         * So maybe CategoryAdapter will have to do it.
         */
        val sound = data.sourceAdapter.currentList.removeAt(data.sourcePosition)
        data.targetAdapter.currentList.add(data.targetPosition, sound)
        if (data.sourceAdapter === data.targetAdapter) {
            data.sourceAdapter.notifyItemMoved(data.sourcePosition, data.targetPosition)
        } else {
            data.sourceAdapter.notifyItemRemoved(data.sourcePosition)
            data.targetAdapter.notifyItemInserted(data.targetPosition)
        }
    }

    private fun getDataHolder(event: DragEvent, targetView: View): DataHolder? {
        try {
            val sourceView = event.localState as View
            val sourceList = sourceView.parent as RecyclerView
            val sourcePosition = sourceView.tag as Int
            val sourceAdapter = sourceList.adapter as SoundAdapter
            val targetList = targetView.parent as RecyclerView
            val targetPosition = targetView.tag as Int
            val targetAdapter = targetList.adapter as SoundAdapter

            return DataHolder(sourceAdapter, sourcePosition, targetAdapter, targetPosition)
        } catch (e: TypeCastException) {
            Sentry.captureException(e)
        }
        return null
    }


    data class DataHolder(
            val sourceAdapter: SoundAdapter,
            val sourcePosition: Int,
            val targetAdapter: SoundAdapter,
            val targetPosition: Int
    )

    companion object {
        const val LOG_TAG = "SoundDragListener"
    }
}