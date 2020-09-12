package us.huseli.soundboard_kotlin.interfaces

interface ItemDragHelperAdapter<T: OrderableItem> {
    val currentList: MutableList<T>
    //fun getMutableList(): MutableList<T>
    // fun getCurrentList(): MutableList<T>
    //fun onItemMove(fromPosition: Int, toPosition: Int): Any
    fun submitList(list: List<T>)
    //fun onItemMoved(fromPosition: Int, toPosition: Int)
    fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    fun onItemsReordered(): Any
}