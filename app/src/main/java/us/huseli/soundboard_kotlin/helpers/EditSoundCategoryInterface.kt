package us.huseli.soundboard_kotlin.helpers

import us.huseli.soundboard_kotlin.data.Category

interface EditSoundCategoryInterface {
    fun onSoundCategoryDialogSave(category: Category)
    fun showSoundCategoryEditDialog(category: Category?)
}