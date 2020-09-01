package us.huseli.soundboard_kotlin.interfaces

interface ItemDragHelperAdapter<T: OrderableItem> {
    fun getMutableList(): MutableList<T>
    fun onItemMove(fromPosition: Int, toPosition: Int)
    //fun onItemMoved(fromPosition: Int, toPosition: Int)
    //fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    fun onItemsReordered(newList: MutableList<T>)
}