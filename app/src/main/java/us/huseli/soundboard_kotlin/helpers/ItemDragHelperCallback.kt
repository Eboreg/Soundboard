package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.interfaces.OrderableItem
import java.util.*

open class ItemDragHelperCallback(dragDirs: Int, private val adapter: ItemDragHelperAdapter<OrderableItem>) : ItemTouchHelper.SimpleCallback(dragDirs, 0) {
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
        dragFrom = null
        dragTo = null
        if (dragFromFinal != null && dragToFinal != null && dragFromFinal != dragToFinal) {
            val list = adapter.getMutableList()
            if (dragFromFinal < dragToFinal)
                for (i in dragFromFinal until dragToFinal) Collections.swap(list, i, i + 1)
            else
                for (i in dragFromFinal downTo dragToFinal + 1) Collections.swap(list, i, i - 1)
            adapter.onItemsReordered(list)
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}