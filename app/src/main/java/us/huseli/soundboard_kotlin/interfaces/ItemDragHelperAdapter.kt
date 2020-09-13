package us.huseli.soundboard_kotlin.interfaces

interface ItemDragHelperAdapter<T: OrderableItem> {
    val currentList: MutableList<T>
    fun submitList(list: List<T>)
    fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    fun onItemsReordered(): Any
}