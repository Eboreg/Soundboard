package us.huseli.soundboard.interfaces

interface EditCategoryInterface {
    fun showCategoryEditDialog(id: Int): Any?
    fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int): Any?
    fun showCategorySortDialog(id: Int, name: String): Any?
}