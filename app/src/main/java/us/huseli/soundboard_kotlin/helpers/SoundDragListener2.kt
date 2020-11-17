package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import android.view.DragEvent
import android.view.View
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter
import us.huseli.soundboard_kotlin.adapters.SoundAdapter
import us.huseli.soundboard_kotlin.data.DraggedSound

/**
 * Owned by CategoryAdapter.ViewHolder.
 * view = RecyclerView with SoundAdapter
 */
class SoundDragListener2(private val soundAdapter: SoundAdapter, private val categoryViewHolder: CategoryAdapter.ViewHolder) : View.OnDragListener {
    private val hashCode = Integer.toHexString(System.identityHashCode(this))

    private var logNumber = 0
    private var isSourceAdapter = false
    private var isTargetAdapter = false
    private var lastItemId: Long? = null
    private var previousViewHolder: SoundAdapter.SoundViewHolder? = null

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
         * Drag starts (ACTION_DRAG_STARTED):
         * - If adapter is source adapter: remove draggedSound from adapter
         *
         * draggedSound enters sound-recyclerview (ACTION_DRAG_ENTERED):
         * - Expand its category
         * - Add its EmptySound
         *
         * draggedSound exits sound-recyclerview (ACTION_DRAG_EXITED):
         * - Remove its EmptySound
         *
         * draggedSound moves on top of a Sound viewholder (ACTION_DRAG_LOCATION):
         * - If viewholder contains draggedSound: do nothing (??)
         * - If viewholder contains EmptySound: do nothing
         * - If viewholder contains another Sound:
         *   - Move adapter's EmptySound to "moved-over" sound's position
         *
         * draggedSound drops (ACTION_DROP):
         * - Move sound to current position
         * - Update adapter's db
         *
         * Drag ends (ACTION_DRAG_ENDED):
         * - Remove adapter's EmptySound
         * - If no drop has occurred (draggedSound.isDragged == true):
         *   - If adapter was source adapter: put draggedSound back at original position
         * - Otherwise
         *   - If adapter was draggedSound's source adapter, but isn't its new one: Update db
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
            DragEvent.ACTION_DRAG_ENDED -> onDragEnded(event, view, draggedSound)
            else -> {
                dumpLog(event.action.toString(), event)
                false
            }
        }
    }

    private fun onDragStarted(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        isDragging = true
        draggedSound.listeners.add(this)
        if (soundAdapter.containsSound(draggedSound.sound)) {
            dumpLog("ACTION_DRAG_STARTED", event, "Adapter is source adapter", mapOf("view" to view))
            isSourceAdapter = true
            //soundAdapter.hideSound(draggedSound.sound)
        } else dumpLog("ACTION_DRAG_STARTED", event, "Adapter is NOT source adapter", mapOf("view" to view))
        return true
    }

    private fun onDragEntered(event: DragEvent, view: View): Boolean {
        dumpLog("ACTION_DRAG_ENTERED", event, mapOf("view" to view))
        isTargetAdapter = true
        if (wasCollapsed) soundAdapter.expandCategory()
        if (soundAdapter.isEmpty()) categoryViewHolder.showDropContainer()
        return true
    }

    private fun onDragExited(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        dumpLog("ACTION_DRAG_EXITED", event, mapOf("view" to view))
        isTargetAdapter = false
        if (wasCollapsed) soundAdapter.collapseCategory()
        categoryViewHolder.hideDropContainer()
        draggedSound.currentAdapterPosition = -1
        return true
    }

    private fun onDragLocation(event: DragEvent, draggedSound: DraggedSound): Boolean {
        if (!soundAdapter.isEmpty() && draggedSound.state == DraggedSound.IDLE) {
            draggedSound.state = DraggedSound.MOVING
            val (x, y) = getXY(event)
            soundAdapter.getSoundViewHolderUnder(x, y)?.let { viewHolder ->
                if (viewHolder.itemId != lastItemId) {
                    //if (viewHolder.itemId != lastItemId && viewHolder.itemId.toInt() != draggedSound.soundId) {
                    lastItemId = viewHolder.itemId
                    /*
                    val toPosition = if (draggedSound.currentAdapterPosition in 0..viewHolder.adapterPosition)
                        viewHolder.adapterPosition + 1 else viewHolder.adapterPosition
                     */
                    // TODO: Make it better
                    draggedSound.currentAdapterPosition = if (viewHolder.adapterPosition > 0)
                        viewHolder.adapterPosition - 1 else viewHolder.adapterPosition
                    dumpLog(
                            "ACTION_DRAG_LOCATION", event,
                            "Move $draggedSound over ${draggedSound.currentAdapterPosition}",
                            mapOf("viewHolder" to viewHolder))

                    // Remove any previous redmarkings
                    soundAdapter.removeMarksForDrop()
                    previousViewHolder = viewHolder
                    /**
                     * Being over another Sound means that, were we to drop here, dragged Sound
                     * would take that Sound's position. So redmark the area between the other
                     * sound and the sound before it, if any.
                     */
                    soundAdapter.markSoundsForDrop(viewHolder)
                    //draggedSound.currentAdapterPosition = viewHolder.adapterPosition
                }
            }
            draggedSound.state = DraggedSound.IDLE
        }
        return true
    }

    private fun onDrop(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        dumpLog("ACTION_DROP", event, "$draggedSound dropped over view=$view; updating DB")
        categoryViewHolder.hideDropContainer()
        soundAdapter.insertOrMoveSound(draggedSound.sound, draggedSound.currentAdapterPosition)
        soundAdapter.removeMarksForDrop()
        //soundAdapter.updateDb()
        return true
    }

    private fun onDragEnded(event: DragEvent, view: View, draggedSound: DraggedSound): Boolean {
        dumpLog("ACTION_DRAG_ENDED", event, mapOf("view" to view))
        reset()
        draggedSound.listeners.remove(this)
//        soundAdapter.cancelDrop()
//        soundAdapter.updateDb()
        return true
    }

    private fun reset() {
        logNumber = 0
        isSourceAdapter = false
        isTargetAdapter = false
        lastItemId = null
        isDragging = false
        previousViewHolder = null
    }


    companion object {
        const val LOG_TAG = "SoundDragListener2"
    }
}