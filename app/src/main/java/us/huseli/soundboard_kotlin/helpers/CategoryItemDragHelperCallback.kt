package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper

@Suppress("UNCHECKED_CAST")
class CategoryItemDragHelperCallback : ItemDragHelperCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN) {
    override fun isLongPressDragEnabled() = false
}