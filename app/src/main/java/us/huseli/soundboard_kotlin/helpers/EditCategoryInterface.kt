package us.huseli.soundboard_kotlin.helpers

import us.huseli.soundboard_kotlin.data.Category

interface EditCategoryInterface {
    fun onCategoryDialogSave(category: Category)
    fun showCategoryEditDialog(categoryId: Int?)
    fun showCategoryDeleteDialog(categoryId: Int)
}