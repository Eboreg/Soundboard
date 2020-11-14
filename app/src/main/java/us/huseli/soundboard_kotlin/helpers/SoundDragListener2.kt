package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import android.view.DragEvent
import android.view.View
import us.huseli.soundboard_kotlin.data.DraggedSound
import us.huseli.soundboard_kotlin.interfaces.SoundDragCallback2

/**
 * Owned by CategoryAdapter.ViewHolder.
 * view = RecyclerView with SoundAdapter
 */
class SoundDragListener2(private val callback: SoundDragCallback2) : View.OnDragListener {
    private val hashCode = Integer.toHexString(System.identityHashCode(this))

    private var logNumber = 0
    private var isSourceAdapter = false
    private var isTargetAdapter = false
    private var lastItemId: Long? = null

    var wasCollapsed = false
    var isDragging = false

    private fun dumpLog(tag: String?, event: DragEvent?, comment: String?, extra: Map<String, Any?>?) {
        var output = "${hashCode}:$logNumber  "
        if (tag != null) output += "$tag: "
        if (comment != null) Log.d(LOG_TAG, output + comment)
        extra?.forEach { Log.d(LOG_TAG, output + "${it.key}=${it.value}") }
        if (event != null) Log.d(LOG_TAG, output + "event=$event")
        Log.d(LOG_TAG, output + "callback=$callback")
        logNumber++
    }

    private fun dumpLog(tag: String, event: DragEvent, comment: String) = dumpLog(tag, event, comment, null)

    private fun dumpLog(tag: String, event: DragEvent) = dumpLog(tag, event, null, null)

    private fun dumpLog(tag: String, event: DragEvent, extra: Map<String, Any?>) = dumpLog(tag, event, null, extra)

    private fun getXY(event: DragEvent): Pair<Float, Float> {
        // Have to correct y value for the height of the category header
        val y = if (event.y >= callback.getYOffset()) event.y - callback.getYOffset() else event.y
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
        val draggedSound = event.localState as DraggedSound

        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                isDragging = true
                if (callback.containsSound(draggedSound.soundId)) {
                    dumpLog("ACTION_DRAG_STARTED", event, "Adapter is source adapter", mapOf("view" to view))
                    isSourceAdapter = true
                    callback.hideSound(draggedSound.soundId)
                } else dumpLog("ACTION_DRAG_STARTED", event, "Adapter is NOT source adapter", mapOf("view" to view))
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                dumpLog("ACTION_DRAG_ENTERED", event, mapOf("view" to view))
                callback.expandCategory()
                callback.insertOrMoveSound(draggedSound.soundId)
                //callback.addEmptySoundIfNecessary()
                true
//                false
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                dumpLog("ACTION_DRAG_EXITED", event, mapOf("view" to view))
                if (wasCollapsed) callback.collapseCategory()
                draggedSound.originalAdapterPosition = -1
                callback.removeSound(draggedSound.soundId)
                //callback.removeEmptySound()
                true
//                false
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                if (draggedSound.state == DraggedSound.IDLE) {
                    draggedSound.state = DraggedSound.MOVING
                    val (x, y) = getXY(event)
                    callback.getSoundViewHolderUnder(x, y)?.let { viewHolder ->
                        if (viewHolder.itemId != lastItemId ) {
                        //if (viewHolder.itemId != lastItemId && viewHolder.itemId.toInt() != draggedSound.soundId) {
                            lastItemId = viewHolder.itemId
                            val toPosition = if (draggedSound.originalAdapterPosition <= viewHolder.adapterPosition)
                                viewHolder.adapterPosition + 1 else viewHolder.adapterPosition
                            dumpLog(
                                    "ACTION_DRAG_LOCATION", event, "Move $draggedSound to $toPosition",
                                    mapOf("viewHolder" to viewHolder))
                            callback.insertOrMoveSound(draggedSound.soundId, toPosition)
                            draggedSound.originalAdapterPosition = toPosition
                            //callback.moveEmptySound(viewHolder.adapterPosition)
                        }
                    }
                    draggedSound.state = DraggedSound.IDLE
                }
                true
            }
            DragEvent.ACTION_DROP -> {
                isTargetAdapter = true
                val (x, y) = getXY(event)
                dumpLog("ACTION_DROP", event, "$draggedSound dropped over view=$view, inserting it at x=$x, y=$y")
                draggedSound.stop()

                callback.insertOrMoveSound(draggedSound.soundId, x, y)
                callback.showSound(draggedSound.soundId)
                /*
                callback.getSoundViewHolderUnder(x, y)?.let { viewHolder ->
                    dumpLog(
                            "ACTION_DROP", event, "Insert sound ${draggedSound.soundId} at position ${viewHolder.adapterPosition}",
                            mapOf("viewHolder" to viewHolder))
                    callback.insertSoundAndSave(draggedSound.soundId, viewHolder.adapterPosition)
                } ?: run {
                    dumpLog("ACTION_DROP", event, "Insert sound ${draggedSound.soundId} last")
                    callback.insertSoundAndSave(draggedSound.soundId)
                }
                 */

                // Will add/move sound to where adapter's EmptySound is now
                // callback.insertSoundAndSave(draggedSound.soundId)
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                dumpLog("ACTION_DRAG_ENDED", event, mapOf("view" to view))
                //callback.removeEmptySound()
                if (draggedSound.isDragged) {
                    if (isSourceAdapter) {
                        dumpLog("ACTION_DRAG_ENDED", event, "No drop occurred and this is source adapter!")
                        draggedSound.stop()
                        callback.showSound(draggedSound.soundId)
                        //callback.insertSoundAndSave(draggedSound.soundId, draggedSound.originalAdapterPosition)
                    }
                } else if (isSourceAdapter && !isTargetAdapter) {
                    dumpLog("ACTION_DRAG_ENDED", event, "Drop occurred, this is source adapter but not target adapter")
                    //callback.updateDb()
                }
                reset()
                //false
                true
            }
            else -> {
                dumpLog(event.action.toString(), event)
                false
            }
        }
    }

    private fun reset() {
        logNumber = 0
        isSourceAdapter = false
        isTargetAdapter = false
        lastItemId = null
        isDragging = false
    }


    companion object {
        const val LOG_TAG = "SoundDragListener2"
    }
}