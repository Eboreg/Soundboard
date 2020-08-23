package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter

class CategoryItemDragHelperCallback(adapter: CategoryAdapter) :
        ItemDragHelperCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, adapter) {
    override fun isLongPressDragEnabled() = false
}