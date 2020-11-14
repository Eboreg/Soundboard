package us.huseli.soundboard_kotlin.interfaces

import us.huseli.soundboard_kotlin.adapters.SoundAdapter

interface SoundDragCallback2 {
    fun addEmptySoundIfNecessary(): Any?
    fun collapseCategory(): Any?
    fun containsSound(soundId: Int): Boolean
    fun expandCategory(): Any?
    fun getSoundViewHolderUnder(x: Float, y: Float): SoundAdapter.SoundViewHolder?
    fun getYOffset(): Float
    fun hideSound(soundId: Int): Any?
    fun insertOrMoveSound(soundId: Int): Any?
    fun insertOrMoveSound(soundId: Int, toPosition: Int)
    fun insertOrMoveSound(soundId: Int, x: Float, y: Float): Any?
    fun moveEmptySound(toPosition: Int): Any?
    fun removeEmptySound(): Any?
    fun removeSound(soundId: Int): Any?
    fun showSound(soundId: Int): Any?
}