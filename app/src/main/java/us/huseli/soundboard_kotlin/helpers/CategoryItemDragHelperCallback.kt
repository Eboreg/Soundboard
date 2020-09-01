package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

@Suppress("UNCHECKED_CAST")
class CategoryItemDragHelperCallback(adapter: CategoryAdapter) :
        ItemDragHelperCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, adapter as ItemDragHelperAdapter<OrderableItem>) {
    override fun isLongPressDragEnabled() = false
}