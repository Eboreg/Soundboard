package us.huseli.soundboard.interfaces

import us.huseli.soundboard.data.Sound

interface EditSoundInterface {
    fun showSoundAddDialog()
    fun showSoundDeleteDialog(sounds: List<Sound>)
    fun showSoundEditDialog(sound: Sound)
    fun showSoundEditDialog(sounds: List<Sound>)
}