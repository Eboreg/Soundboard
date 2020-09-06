package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.adapters.SoundAdapter

@Suppress("UNCHECKED_CAST")
class SoundItemDragHelperCallback : ItemDragHelperCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder is SoundAdapter.ViewHolder && actionState != ItemTouchHelper.ACTION_STATE_IDLE) viewHolder.onItemSelected()
        super.onSelectedChanged(viewHolder, actionState)
    }
}