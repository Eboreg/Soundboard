package us.huseli.soundboard_kotlin.helpers

import us.huseli.soundboard_kotlin.data.SoundCategory

interface EditSoundCategoryInterface {
    fun onSoundCategoryDialogSave(category: SoundCategory)
    fun showSoundCategoryEditDialog(category: SoundCategory?)
}