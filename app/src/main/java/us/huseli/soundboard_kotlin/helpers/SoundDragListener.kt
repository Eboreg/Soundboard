package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.adapters.SoundAdapter
import us.huseli.soundboard_kotlin.interfaces.SoundDragCallback
import us.huseli.soundboard_kotlin.viewmodels.DraggedSoundViewModel

/**
 * Owned by CategoryAdapter.ViewHolder.
 * view = RecyclerView with SoundAdapter
 */
class SoundDragListener(
        private val recyclerView: RecyclerView,
        private val callback: SoundDragCallback) : View.OnDragListener {
    private var currentViewHolder: SoundAdapter.ViewHolder? = null
    private var isDragged = false

    override fun onDrag(view: View, event: DragEvent): Boolean {
        /**
         * Sound enters sound-recyclerview:
         * - If there is another sound view "under" it: shift that one up 1 pos, add dragged sound
         * to empty/transparent holder at current pos
         * at current pos
         * - If no sound view under: just add dragged sound to empty/transparent holder last in
         * adapter
         *
         * Sound exits sound-recyclerview:
         * Remove any viewholder containing dragged sound from this recyclerview
         *
         * Sound drops:
         * Make sound viewholder visible & update db
         *
         * Drag ends without drop:
         * Same as on drop (could be changed if it causes some trouble)
         */
        val viewModel = event.localState as DraggedSoundViewModel
        isDragged = true

        Log.d(LOG_TAG, "event=$event, viewModel=$viewModel")

        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                // We are moving over a Sound RecyclerView
                callback.expandCategory()
                // Have to correct y value for the height of the category header
                val y = if (event.y >= recyclerView.y) event.y - recyclerView.y else event.y
                val childView = recyclerView.findChildViewUnder(event.x, y)
                val viewHolder = childView?.let { recyclerView.findContainingViewHolder(it) }
                if (viewHolder is SoundAdapter.ViewHolder) {
                    // We are moving over a child View with a Sound ViewHolder
                    if (viewHolder != currentViewHolder) {
                        // ViewHolder is not the same as the previous one
                        Log.i(LOG_TAG, "ACTION_DRAG_LOCATION: viewHolder $viewHolder != currentViewHolder $currentViewHolder")
                        currentViewHolder = viewHolder
                        if (viewHolder.sound != viewModel.sound) {
                            // ViewHolder's Sound is not the dragged Sound, so move dragged
                            // Sound into its position
                            Log.i(LOG_TAG, "ACTION_DRAG_LOCATION: Moving ${viewModel.sound} to position ${viewHolder.adapterPosition} in $callback")
                            if (!callback.moveSound(viewModel.sound, viewHolder.adapterPosition))
                                addSoundToAdapter(viewModel, viewHolder.adapterPosition)
                        }
                    }
                } else {
                    if (recyclerView.hashCode() != viewModel.recyclerViewHashCode) {
                        // We are not over a child View, or it doesn't have a Sound ViewHolder
                        // So just move dragged Sound to last position in adapter
                        Log.i(LOG_TAG, "ACTION_DRAG_LOCATION: Not over a child View, moving ${viewModel.sound} last in $callback")
                        addSoundToAdapter(viewModel)
                    }
                }
            }
            DragEvent.ACTION_DROP -> {
                Log.i(LOG_TAG, "ACTION_DROP: ${viewModel.sound} dropped over view=$view, callback=${callback}")
                stopAndSave(viewModel)
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                if (isDragged) Log.i(LOG_TAG, "ACTION_DRAG_ENDED: isDragged=true; no drop occurred?")
                stopAndSave(viewModel)
            }
        }
        return true
    }

    private fun stopAndSave(viewModel: DraggedSoundViewModel) {
        if (isDragged) {
            isDragged = false
            callback.updateSoundDb()
            viewModel.stop()
        }
    }

    private fun addSoundToAdapter(viewModel: DraggedSoundViewModel, position: Int?) {
        Log.i(LOG_TAG, "Adding ${viewModel.sound} to $callback on position $position")
        callback.removeSoundGlobal(viewModel.sound)
        callback.addSound(viewModel.sound, position)
        viewModel.recyclerViewHashCode = recyclerView.hashCode()
    }

    private fun addSoundToAdapter(viewModel: DraggedSoundViewModel) = addSoundToAdapter(viewModel, null)


    companion object {
        const val LOG_TAG = "SoundDragListener"
    }
}