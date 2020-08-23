package us.huseli.soundboard_kotlin.interfaces

interface EditCategoryInterface {
    fun showCategoryAddDialog()
    fun showCategoryEditDialog(categoryId: Int)
    fun showCategoryDeleteDialog(categoryId: Int)
}