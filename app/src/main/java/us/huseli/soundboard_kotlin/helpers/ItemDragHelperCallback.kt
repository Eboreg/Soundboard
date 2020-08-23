package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter

open class ItemDragHelperCallback(dragDirs: Int, private val adapter: ItemDragHelperAdapter) : ItemTouchHelper.SimpleCallback(dragDirs, 0) {
    private var dragFrom: Int? = null
    private var dragTo: Int? = null

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (dragFrom == null) dragFrom = viewHolder.adapterPosition
        dragTo = target.adapterPosition
        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val dragFromFinal = dragFrom
        val dragToFinal = dragTo
        if (dragFromFinal != null && dragToFinal != null && dragFromFinal != dragToFinal)
            adapter.onItemMoved(dragFromFinal, dragToFinal)
        dragFrom = null
        dragTo = null
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}