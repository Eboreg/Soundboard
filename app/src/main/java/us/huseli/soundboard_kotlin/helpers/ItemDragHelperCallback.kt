package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

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

        recyclerView.adapter?.let {
            @Suppress("UNCHECKED_CAST") val adapter = it as ItemDragHelperAdapter<OrderableItem>
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            val list = adapter.getCurrentList().sortedBy { item -> item.order }

            list.forEachIndexed { index, item ->
                if (index == fromPosition) item.order = toPosition
                else if (fromPosition > toPosition && index >= toPosition && index < fromPosition) item.order++
                else if (fromPosition < toPosition && index > fromPosition && index <= toPosition) item.order--
                else item.order = index
            }

/*
            if (fromPosition < toPosition)
                for (i in (fromPosition + 1)..toPosition) list[i].order--
            else
                for (i in toPosition until fromPosition) list[i].order++
*/
            //list[fromPosition].order = toPosition
            adapter.notifyItemMoved(fromPosition, toPosition)
        }

/*
        recyclerView.adapter?.let { adapter ->
            if (adapter is ItemDragHelperAdapter<*>)
                adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        }
*/
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        val dragFromFinal = dragFrom
        val dragToFinal = dragTo
        dragFrom = null
        dragTo = null
        (recyclerView.adapter as ItemDragHelperAdapter<*>).onItemsReordered()
/*
        recyclerView.adapter?.let {
            @Suppress("UNCHECKED_CAST") val adapter = it as ItemDragHelperAdapter<OrderableItem>
            if (dragFromFinal != null && dragToFinal != null && dragFromFinal != dragToFinal) {
                //val list = adapter.getMutableList()
                val list = adapter.getCurrentList()
                Log.i(GlobalApplication.LOG_TAG,
                        "ItemDragHelperCallback ${this.hashCode()} clearView: drag from $dragFromFinal to $dragToFinal, list = $list, " +
                                "recyclerView ${recyclerView.hashCode()}, adapter ${adapter.hashCode()}, viewHolder ${viewHolder.hashCode()}")
                try {
                    list[dragFromFinal].order = dragToFinal
                    if (dragFromFinal < dragToFinal)
                        //for (i in dragFromFinal until dragToFinal) Collections.swap(list, i, i + 1)
                        for (i in (dragFromFinal + 1)..dragToFinal) list[i].order--
                    else
                        //for (i in dragFromFinal downTo dragToFinal + 1) Collections.swap(list, i, i - 1)
                        for (i in dragToFinal until dragFromFinal) list[i].order++
                    //list.forEachIndexed { index, item -> item.order = index }
                    //adapter.submitList(list) { adapter.onItemsReordered() }
                    adapter.onItemsReordered()
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(GlobalApplication.LOG_TAG, e.toString())
                }
            }
        }
*/
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}