package us.huseli.soundboard_kotlin.interfaces

import us.huseli.soundboard_kotlin.adapters.SoundAdapter
import us.huseli.soundboard_kotlin.data.Sound

interface SoundDragCallback2 {
    //fun addEmptySoundIfNecessary(): Any?
    fun cancelDrop(): Any?
    fun collapseCategory(): Any?
    fun containsSound(sound: Sound): Boolean
    fun expandCategory(): Any?
    fun getAdapter(): SoundAdapter
    fun getSoundViewHolderUnder(x: Float, y: Float): SoundAdapter.SoundViewHolder?
    fun getYOffset(): Float
    fun hideSound(sound: Sound): Any?
    //fun insertOrMoveSound(sound: Sound): Any?
    fun insertOrMoveSound(sound: Sound, toPosition: Int)
    //fun insertOrMoveSound(sound: Sound, x: Float, y: Float): Any?
    //fun moveEmptySound(toPosition: Int): Any?
    //fun removeEmptySound(): Any?
    //fun removeSound(sound: Sound): Any?
    fun showSound(sound: Sound): Any?
    fun updateDb(): Any?
    fun markSoundsForDrop(viewHolder: SoundAdapter.SoundViewHolder): Any?
    fun removeMarksForDrop(): Any?
    fun isEmpty(): Boolean
}