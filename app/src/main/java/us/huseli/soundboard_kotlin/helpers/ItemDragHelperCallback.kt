package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.interfaces.OrderableItem
import java.util.*

open class ItemDragHelperCallback(dragDirs: Int) : ItemTouchHelper.SimpleCallback(dragDirs, 0) {
    private var dragFrom: Int? = null
    private var dragTo: Int? = null

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (dragFrom == null) dragFrom = viewHolder.adapterPosition
        dragTo = target.adapterPosition

        Log.d(GlobalApplication.LOG_TAG,
                "ItemDragHelperCallback ${this.hashCode()} onMove: " +
                        "viewHolder ${viewHolder.hashCode()}, target ${target.hashCode()}, " +
                        "fromPosition ${viewHolder.adapterPosition}, " +
                        "toPosition ${target.adapterPosition}, recyclerView ${recyclerView.hashCode()}")
        recyclerView.adapter?.let { adapter ->
            if (adapter is ItemDragHelperAdapter<*>)
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        }
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val dragFromFinal = dragFrom
        val dragToFinal = dragTo
        dragFrom = null
        dragTo = null
        recyclerView.adapter?.let {
            @Suppress("UNCHECKED_CAST") val adapter = it as ItemDragHelperAdapter<OrderableItem>
            if (dragFromFinal != null && dragToFinal != null && dragFromFinal != dragToFinal) {
                val list = adapter.getMutableList()
                Log.i(GlobalApplication.LOG_TAG,
                        "ItemDragHelperCallback ${this.hashCode()} clearView: drag from $dragFromFinal to $dragToFinal, list = $list, " +
                                "recyclerView ${recyclerView.hashCode()}, adapter ${adapter.hashCode()}, viewHolder ${viewHolder.hashCode()}")
                try {
                    if (dragFromFinal < dragToFinal)
                        for (i in dragFromFinal until dragToFinal) Collections.swap(list, i, i + 1)
                    else
                        for (i in dragFromFinal downTo dragToFinal + 1) Collections.swap(list, i, i - 1)
                    adapter.onItemsReordered(list)
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(GlobalApplication.LOG_TAG, e.toString())
                }
            }
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}