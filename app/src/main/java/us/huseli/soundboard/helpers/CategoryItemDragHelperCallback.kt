package us.huseli.soundboard.helpers

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.adapters.CategoryAdapter
import java.util.*

class CategoryItemDragHelperCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG,
            "ItemDragHelperCallback ${this.hashCode()} onMove: " +
                    "viewHolder ${viewHolder.hashCode()}, target ${target.hashCode()}, " +
                    "fromPosition ${viewHolder.bindingAdapterPosition}, " +
                    "toPosition ${target.bindingAdapterPosition}, recyclerView ${recyclerView.hashCode()}"
        )

        recyclerView.adapter?.let { adapter ->
            if (adapter is CategoryAdapter) {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition
                val mutableList = adapter.currentList.toMutableList()

                if (fromPosition < toPosition)
                    for (i in fromPosition until toPosition) Collections.swap(mutableList, i, i + 1)
                else
                    for (i in fromPosition downTo toPosition + 1) Collections.swap(
                        mutableList,
                        i,
                        i - 1
                    )
                adapter.submitList(mutableList)
                return true
            }
        }
        return false
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // By the time we get here, the list should be in the correct (new) order
        (recyclerView.adapter as? CategoryAdapter)?.onItemsReordered()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled() = false


    companion object {
        const val LOG_TAG = "catItemDragHelper"
    }
}