package us.huseli.soundboard_kotlin.interfaces

interface EditCategoryInterface {
    fun showCategoryEditDialog(categoryId: Int): Any?
    fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int): Any?
}