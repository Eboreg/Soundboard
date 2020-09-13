package us.huseli.soundboard_kotlin.interfaces

interface EditSoundInterface {
    fun showSoundEditDialog(soundId: Int, categoryId: Int?)
    fun showSoundDeleteDialog(soundId: Int, soundName: String?)
}