package us.huseli.soundboard_kotlin

import us.huseli.soundboard_kotlin.data.SoundViewModel

interface EditSoundInterface {
    fun onSoundDialogSave(soundViewModel: SoundViewModel)
    fun showEditDialog(soundViewModel: SoundViewModel)
}