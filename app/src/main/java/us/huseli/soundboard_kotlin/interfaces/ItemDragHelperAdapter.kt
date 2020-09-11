package us.huseli.soundboard_kotlin.interfaces

interface ItemDragHelperAdapter<T: OrderableItem> {
    fun getMutableList(): MutableList<T>
    fun getCurrentList(): List<T>
    fun onItemMove(fromPosition: Int, toPosition: Int): Any
    fun submitList(list: List<T>?, commitCallback: Runnable?)
    //fun onItemMoved(fromPosition: Int, toPosition: Int)
    fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    fun onItemsReordered(): Any
}