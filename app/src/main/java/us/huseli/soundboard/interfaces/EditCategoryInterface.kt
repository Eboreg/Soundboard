package us.huseli.soundboard.interfaces

import us.huseli.soundboard.data.Category

interface EditCategoryInterface {
    fun showCategoryAddDialog(): Any?
    fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int): Any?
    fun showCategoryEditDialog(category: Category)
}