package us.huseli.soundboard_kotlin.interfaces

interface ItemDragHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int)
    fun onItemMoved(fromPosition: Int, toPosition: Int)
}