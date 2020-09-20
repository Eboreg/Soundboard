package us.huseli.soundboard_kotlin.interfaces

import us.huseli.soundboard_kotlin.data.Sound

interface EditSoundInterface {
    fun showSoundEditDialog(sound: Sound): Any?
    fun showMultipleSoundEditDialog(soundsIds: List<Int>): Any?
    fun showSoundDeleteDialog(soundId: Int, soundName: String?): Any?
}