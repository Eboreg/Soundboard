package us.huseli.soundboard_kotlin.helpers

/**
 * Owned by CategoryAdapter.ViewHolder.
 * view = RecyclerView with SoundAdapter
 */
/*
class SoundDragListener(private val callback: SoundDragCallback) : View.OnDragListener {
    private val moveThreshold = 0.5f
    private val recyclerView = callback.getRecyclerView()
    private val hashCode = Integer.toHexString(System.identityHashCode(this))

    private var currentViewHolder: SoundAdapter.SoundViewHolder? = null
    private var draggedSound: DraggedSound? = null
    private var logNumber = 0

    var wasCollapsed: Boolean? = null

    private fun shouldMove(eventX: Float, eventY: Float): Boolean {
        // TODO: Lots of false positives after move but before drop, when items have switched place
        draggedSound?.let {
            if (it.dragStartX != null && it.dragStartY != null) {
                val dX = abs(eventX - it.dragStartX!!) - it.currentX
                val dY = abs(eventY - it.dragStartY!!) - it.currentY
                return abs(dX) > it.width * moveThreshold || abs(dY) > it.height * moveThreshold
            }
        }
        return true
    }

    private fun dumpLog(tag: String?, event: DragEvent?, comment: String?, extra: Map<String, Any?>?) {
        var output = "${hashCode}:$logNumber  "
        if (tag != null) output += "$tag: "
        if (comment != null) output += "$comment | "
        extra?.forEach { Log.d(LOG_TAG, output + "${it.key}=${it.value}") }
        if (event != null) Log.d(LOG_TAG, output + "event=$event")
        Log.d(LOG_TAG, output + "recyclerView=$recyclerView")
        Log.d(LOG_TAG, output + "currentViewHolder=$currentViewHolder")
        Log.d(LOG_TAG, output + "draggedSound=$draggedSound")
        Log.d(LOG_TAG, output + "callback=$callback")
        logNumber++
    }

    private fun dumpLog(tag: String, event: DragEvent, comment: String) = dumpLog(tag, event, comment, null)

    private fun dumpLog(tag: String, comment: String) = dumpLog(tag, null, comment, null)

    private fun dumpLog(tag: String, event: DragEvent) = dumpLog(tag, event, null, null)

    private fun dumpLog(tag: String, event: DragEvent, extra: Map<String, Any?>) = dumpLog(tag, event, null, extra)

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
        draggedSound = event.localState as DraggedSound

        return when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                // Have to correct y value for the height of the category header
                val y = if (event.y >= recyclerView.y) event.y - recyclerView.y else event.y

                if (draggedSound?.isDragged != true) {
                    dumpLog("ACTION_DRAG_LOCATION", "isDragged!=true, running draggedSound.start(${event.x}, $y)")
                    draggedSound?.start(event.x, y)
                }

                if (draggedSound?.recyclerViewHashCode != recyclerView.hashCode() || shouldMove(event.x, y)) {
                    //Log.i(LOG_TAG, "ACTION_DRAG_LOCATION: Should move")
                    val childView = recyclerView.findChildViewUnder(event.x, y)
                    val viewHolder = childView?.let { recyclerView.findContainingViewHolder(it) }
                    dumpLog("ACTION_DRAG_LOCATION", event, mapOf("childView" to childView, "viewHolder" to viewHolder))
                    if (viewHolder is SoundAdapter.SoundViewHolder) {
                        // We are moving over a child View with a Sound ViewHolder
                        if (viewHolder != currentViewHolder) {
                            // ViewHolder is not the same as the previous one
                            dumpLog("ACTION_DRAG_LOCATION", event, "viewHolder != currentViewHolder", mapOf("viewHolder" to viewHolder))
                            currentViewHolder = viewHolder
                            if (viewHolder.viewModel?.sound != draggedSound?.viewModel?.sound) {
                                dumpLog("ACTION_DRAG_LOCATION", event, "viewHolder.sound != draggedSound.sound", mapOf("viewHolder.sound" to viewHolder.viewModel?.sound, "draggedSound.sound" to draggedSound?.viewModel?.sound))
                                // ViewHolder's Sound is not the dragged Sound, so move dragged
                                // Sound into its position
                                dumpLog("ACTION_DRAG_LOCATION", event, "Trying callback.moveSound(${draggedSound?.viewModel?.sound}, ${viewHolder.adapterPosition})")
                                if (!callback.moveSound(draggedSound?.viewModel?.sound, viewHolder.adapterPosition)) {
                                    dumpLog("ACTION_DRAG_LOCATION", event, "Unable to move sound internally, running moveSound($viewHolder)")
                                    moveSound(viewHolder)
                                }
                            }
                        }
                    } else {
                        dumpLog("ACTION_DRAG_LOCATION", event, "viewHolder is not SoundAdapter.ViewHolder", mapOf("viewHolder" to viewHolder))
                        if (recyclerView.hashCode() != draggedSound?.recyclerViewHashCode) {
                            // We are not over a child View, or it doesn't have a Sound ViewHolder
                            // So just move dragged Sound to last position in adapter
                            dumpLog("ACTION_DRAG_LOCATION", event, "recyclerView.hashCode() (${recyclerView.hashCode()}) != draggedSound?.recyclerViewHashCode (${draggedSound?.recyclerViewHashCode}")
                            Log.i(LOG_TAG, "ACTION_DRAG_LOCATION: Not over a child View, moving ${draggedSound?.viewModel?.sound} last in $callback")
                            moveSound()
                        }
                    }
                }
                false
            }
            DragEvent.ACTION_DROP -> {
                dumpLog("ACTION_DROP", event, "${draggedSound?.viewModel?.sound} dropped over view=$view")
                stopAndSave()
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                dumpLog("ACTION_DRAG_ENDED", event, mapOf("view" to view))
                if (draggedSound?.isDragged == true) dumpLog("ACTION_DRAG_ENDED", "isDragged=true; no drop occurred?")
                stopAndSave()
                false
            }
            DragEvent.ACTION_DRAG_STARTED -> {
                dumpLog("ACTION_DRAG_STARTED", event, mapOf("view" to view))
                true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                dumpLog("ACTION_DRAG_ENTERED", event, mapOf("view" to view))
                // We are moving over a Sound RecyclerView
                callback.expandCategory()
                false
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                dumpLog("ACTION_DRAG_EXITED", event, mapOf("view" to view))
                if (wasCollapsed == true) callback.collapseCategory()
                false
            }
            else -> {
                dumpLog(event.action.toString(), event)
                false
            }
        }
    }

    private fun stopAndSave() {
        draggedSound?.let {
            if (it.isDragged) {
                dumpLog("stopAndSave()", "isDragged=true; stopping and updating DB")
                it.stop()
                callback.updateSoundDb()
            }
        }
        draggedSound = null
        logNumber = 0
    }

    private fun moveSound(holder: SoundAdapter.SoundViewHolder) {
        draggedSound?.let {
            dumpLog("moveSound(holder)", "Adding ${it.viewModel.sound} to $callback in place of $holder")
            it.recyclerViewHashCode = recyclerView.hashCode()
            it.updatePosition(holder.itemView.left, holder.itemView.top)
            //callback.removeSoundGlobal(it.sound)
            callback.addSound(it.viewModel.sound, holder.adapterPosition)
        }
    }

    private fun moveSound() {
        draggedSound?.let {
            dumpLog("moveSound()", "Adding ${it.viewModel.sound} to $callback")
            //callback.removeSoundGlobal(it.sound)
            val newPosition = callback.addSound(it.viewModel.sound)
            it.recyclerViewHashCode = recyclerView.hashCode()
            recyclerView.findViewHolderForAdapterPosition(newPosition)?.run {
                it.updatePosition(itemView.left, itemView.top)
            }
        }
    }


    companion object {
        const val LOG_TAG = "SoundDragListener"
    }
}
*/
