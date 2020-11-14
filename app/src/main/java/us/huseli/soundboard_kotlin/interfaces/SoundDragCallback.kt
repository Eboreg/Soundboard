package us.huseli.soundboard_kotlin.interfaces

import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.data.Sound

interface SoundDragCallback {
    //fun removeSoundGlobal(sound: Sound): Any?
    fun moveSound(sound: Sound?, position: Int): Boolean  // only used in orig listener
    fun addSound(sound: Sound, position: Int): Int  // only used in orig listener
    fun addSound(sound: Sound): Int  // only used in orig listener
    fun expandCategory(): Any?
    fun updateSoundDb(): Any?  // only used in orig listener
    fun getRecyclerView(): RecyclerView
    fun collapseCategory(): Any?  // only used in orig listener
    fun showEmptySound(): Any?
    fun hideEmptySound(): Any?
    fun moveEmptySoundIfNecessary(x: Float?, y: Float?): Any?
    fun moveSoundIfNecessary(sound: Sound, x: Float?, y: Float?)
}