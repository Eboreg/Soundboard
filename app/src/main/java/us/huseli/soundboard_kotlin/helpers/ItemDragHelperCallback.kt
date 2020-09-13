package us.huseli.soundboard_kotlin.helpers

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.interfaces.OrderableItem
import java.util.*

open class ItemDragHelperCallback(dragDirs: Int) : ItemTouchHelper.SimpleCallback(dragDirs, 0) {

    /**
     * Order of events:
     * 1. User drags item from pos x to pos y
     * 2. We update currentList and notify listeners
     * 3. User releases item at pos z
     * 4. We save new position data via onItemsReordered
     * 5. Room LiveData observer sends us new list, but it's already identical to currentList, so
     *    thanks to DiffUtil UI does not need to be updated!
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        Log.d(GlobalApplication.LOG_TAG,
                "ItemDragHelperCallback ${this.hashCode()} onMove: " +
                        "viewHolder ${viewHolder.hashCode()}, target ${target.hashCode()}, " +
                        "fromPosition ${viewHolder.adapterPosition}, " +
                        "toPosition ${target.adapterPosition}, recyclerView ${recyclerView.hashCode()}")

        recyclerView.adapter?.let {
            @Suppress("UNCHECKED_CAST") val adapter = it as ItemDragHelperAdapter<OrderableItem>
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition

            if (fromPosition < toPosition)
                for (i in fromPosition until toPosition) Collections.swap(adapter.currentList, i, i + 1)
            else
                for (i in fromPosition downTo toPosition + 1) Collections.swap(adapter.currentList, i, i - 1)

            adapter.notifyItemMoved(fromPosition, toPosition)
        }
        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // By the time we get here, the list should be in the correct (new) order
        recyclerView.adapter?.let {
            @Suppress("UNCHECKED_CAST") val adapter = it as ItemDragHelperAdapter<OrderableItem>
            adapter.currentList.forEachIndexed { index, item -> item.order = index }
            adapter.onItemsReordered()
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
}