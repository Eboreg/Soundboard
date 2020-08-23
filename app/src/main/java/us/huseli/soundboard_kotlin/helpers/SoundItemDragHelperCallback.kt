package us.huseli.soundboard_kotlin.helpers

import androidx.recyclerview.widget.ItemTouchHelper
import us.huseli.soundboard_kotlin.adapters.SoundAdapter

class SoundItemDragHelperCallback(adapter: SoundAdapter) :
        ItemDragHelperCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, adapter)