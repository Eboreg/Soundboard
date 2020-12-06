package us.huseli.soundboard.helpers

import android.util.Log
import android.view.DragEvent
import android.view.View
import us.huseli.soundboard.adapters.CategoryAdapter
import us.huseli.soundboard.adapters.SoundAdapter
import us.huseli.soundboard.data.DraggedSound

/**
 * Owned by CategoryAdapter.ViewHolder.
 * view = RecyclerView with SoundAdapter
 */
class SoundDragListener(private val soundAdapter: SoundAdapter, private val categoryViewHolder: CategoryAdapter.CategoryViewHolder) : View.OnDragListener {
    private val hashCode = Integer.toHexString(System.identityHashCode(this))
    private var logNumber = 0

    var wasCollapsed = false
    var isDragging = false

    private fun dumpLog(tag: String?, event: DragEvent?, comment: String?, extra: Map<String, Any?>?) {
        var output = "${hashCode}:$logNumber  "
        if (tag != null) output += "$tag: "
        if (comment != null) Log.d(LOG_TAG, output + comment)
        extra?.forEach { Log.d(LOG_TAG, output + "${it.key}=${it.value}") }
        if (event != null) Log.d(LOG_TAG, output + "event=$event")
        Log.d(LOG_TAG, output + "soundAdapter=$soundAdapter")
        logNumber++
    }

    private fun dumpLog(tag: String, event: DragEvent, comment: String) = dumpLog(tag, event, comment, null)

    private fun dumpLog(tag: String, event: DragEvent) = dumpLog(tag, event, null, null)

    private fun dumpLog(tag: String, event: DragEvent, extra: Map<String, Any?>) = dumpLog(tag, event, null, extra)

    private fun getXY(event: DragEvent): Pair<Float, Float> {
        // Have to correct y value for the height of the category header
        val y = if (event.y >= categoryViewHolder.getYOffset()) event.y - categoryViewHolder.getYOffset() else event.y
        return Pair(event.x, y)
    }

    override fun onDrag(view: View, event: DragEvent): Boolean {
        /**
         * draggedSound enters sound-recyclerview (ACTION_DRAG_ENTERED):
         * - Expand its category
         * - Show its drop container if empty
         *
         * draggedSound exits sound-recyclerview (ACTION_DRAG_EXITED):
         * - Collapse its category if it was collapsed before
         * - Remove any "drop highlight markers" (see ACTION_DRAG_LOCATION)
         * - Hide its drop container
         *
         * draggedSound moves on top of a recyclerview (ACTION_DRAG_LOCATION):
         * - If we are over Sound viewholder: check at what position draggedSound would end up if
         *   we were to drop it now, and highlight the area between that position and the one after
         *
         * draggedSound drops (ACTION_DROP):
         * - Remove any "drop highlight markers" (see ACTION_DRAG_LOCATION)
         * - Move sound to current position
         * - Hide drop container
         * - Remove any "drop highlight markers" (see ACTION_DRAG_LOCATION)
         * - Update adapter's db
         *
         * Drag ends (ACTION_DRAG_ENDED):
         * - Just reset shit
         */
        if (event.localState !is DraggedSound) {
            Log.e(LOG_TAG, "event.localState (${event.localState}) is not DraggedSound")
            return false
        }
        val draggedSound = event.localState as DraggedSound

        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> onDragStarted(event, view, draggedSound)
            DragEvent.ACTION_DRAG_ENTERED -> onDragEntered(event, view)
            DragEvent.ACTION_DRAG_EXITED -> onDragExited(event, view, draggedSound)
            DragEvent.ACTION_DRAG_LOCATION -> onDragLocation(event, draggedSound)
            DragEvent.ACTION_DROP -> onDrop(event, view, draggedSound)
            DragEvent.ACTION_DRAG_ENDED -> onDragEnded(event, view)
            else -> {
                dumpLog(event.action.toString(), event)
                false
            }
        }
    }

    private fun onDragStarted(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        isDragging = true
        if (soundAdapter.containsSound(draggedSound.sound)) {
            dumpLog("ACTION_DRAG_STARTED", event, "Adapter is source adapter", mapOf("view" to view))
        } else dumpLog("ACTION_DRAG_STARTED", event, "Adapter is NOT source adapter", mapOf("view" to view))
        return true
    }

    private fun onDragEntered(event: DragEvent, view: View): Boolean {
        dumpLog("ACTION_DRAG_ENTERED", event, mapOf("view" to view))
        if (wasCollapsed) soundAdapter.expandCategory()
        if (soundAdapter.isEmpty()) categoryViewHolder.showDropContainer()
        return true
    }

    private fun onDragExited(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        dumpLog("ACTION_DRAG_EXITED", event, mapOf("view" to view))
        if (wasCollapsed) soundAdapter.collapseCategory()
        soundAdapter.removeMarksForDrop()
        categoryViewHolder.hideDropContainer()
        draggedSound.currentAdapterPosition = -1
        return true
    }

    private fun onDragLocation(event: DragEvent, draggedSound: DraggedSound): Boolean {
        /**
         * Get the adapter position we would move to, were we to drop right now.
         * We cannot return a ViewHolder, since we might be over the rightmost part of the
         * last viewholder, in which case there is not yet a viewholder at the position where
         * we want to drop.
         */
        if (!soundAdapter.isEmpty() && draggedSound.state == DraggedSound.State.IDLE) {
            draggedSound.state = DraggedSound.State.MOVING
            val (x, y) = getXY(event)
            val adapterPosition = soundAdapter.getAdapterPositionUnder(x, y)
            if (adapterPosition != draggedSound.currentAdapterPosition) {
                draggedSound.currentAdapterPosition = adapterPosition
                dumpLog("ACTION_DRAG_LOCATION", event, "Move $draggedSound to ${draggedSound.currentAdapterPosition}")
                // Remove any previous redmarkings
                soundAdapter.removeMarksForDrop()
                soundAdapter.markSoundsForDrop(adapterPosition)
            }
            draggedSound.state = DraggedSound.State.IDLE
        }
        return true
    }

    private fun onDrop(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        dumpLog("ACTION_DROP", event, "$draggedSound dropped over view=$view; updating DB")
        categoryViewHolder.hideDropContainer()
        soundAdapter.removeMarksForDrop()
        soundAdapter.insertOrMoveSound(draggedSound.sound, draggedSound.currentAdapterPosition)
        return true
    }

    private fun onDragEnded(event: DragEvent, view: View): Boolean {
        dumpLog("ACTION_DRAG_ENDED", event, mapOf("view" to view))
        reset()
        return true
    }

    private fun reset() {
        logNumber = 0
        isDragging = false
    }


    companion object {
        const val LOG_TAG = "SoundDragListener"
    }
}