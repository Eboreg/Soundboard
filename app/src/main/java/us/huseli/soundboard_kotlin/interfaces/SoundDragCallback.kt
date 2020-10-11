package us.huseli.soundboard_kotlin.interfaces

import us.huseli.soundboard_kotlin.data.Sound

interface SoundDragCallback {
    fun removeSoundGlobal(sound: Sound): Any?
    fun moveSound(sound: Sound, position: Int): Boolean
    fun addSound(sound: Sound, position: Int?): Any?
    fun expandCategory(): Any?
    fun updateSoundDb(): Any?
}