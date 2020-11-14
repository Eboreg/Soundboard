package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter
import java.util.*

class CategoryItemDragHelperCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        Log.d(GlobalApplication.LOG_TAG,
                "ItemDragHelperCallback ${this.hashCode()} onMove: " +
                        "viewHolder ${viewHolder.hashCode()}, target ${target.hashCode()}, " +
                        "fromPosition ${viewHolder.adapterPosition}, " +
                        "toPosition ${target.adapterPosition}, recyclerView ${recyclerView.hashCode()}")

        recyclerView.adapter?.let { adapter ->
            if (adapter is CategoryAdapter) {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                val mutableList = adapter.currentList.toMutableList()

                if (fromPosition < toPosition)
                    for (i in fromPosition until toPosition) Collections.swap(mutableList, i, i + 1)
                else
                    for (i in fromPosition downTo toPosition + 1) Collections.swap(mutableList, i, i - 1)
                adapter.submitList(mutableList)
                //adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

/*
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // By the time we get here, the list should be in the correct (new) order
        recyclerView.adapter?.let { adapter ->
            if (adapter is CategoryAdapter) {
                adapter.currentList.forEachIndexed { index, item -> item.order = index }
                adapter.onItemsReordered()
            }
        }
    }

 */

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled() = false
}