package us.huseli.soundboard_kotlin.interfaces

import us.huseli.soundboard_kotlin.data.Category

interface EditCategoryInterface {
    fun onCategoryDialogSave(category: Category)
    fun showCategoryEditDialog(categoryId: Int?)
    fun showCategoryDeleteDialog(categoryId: Int)
}