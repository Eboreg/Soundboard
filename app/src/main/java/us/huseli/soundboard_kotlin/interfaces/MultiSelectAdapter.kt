package us.huseli.soundboard_kotlin.interfaces

interface MultiSelectAdapter<T> {
    fun toggleSelection(position: Int)

    fun clearSelections()

    fun getSelectedItemCount(): Int

    fun getSelectedItems(): List<T>
}