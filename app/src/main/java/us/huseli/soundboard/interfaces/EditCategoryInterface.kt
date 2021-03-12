package us.huseli.soundboard.interfaces

interface EditCategoryInterface {
    fun showCategoryAddDialog(): Any?
    fun showCategoryEditDialog(id: Int): Any?
    fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int): Any?
}