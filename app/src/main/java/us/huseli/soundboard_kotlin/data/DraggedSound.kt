package us.huseli.soundboard_kotlin.data

import us.huseli.soundboard_kotlin.adapters.SoundAdapter
import us.huseli.soundboard_kotlin.helpers.SoundDragListener2

class DraggedSound(val sound: Sound, val originalAdapter: SoundAdapter, private val originalAdapterPosition: Int) {
    //var isDragged = true
    var currentAdapterPosition: Int = originalAdapterPosition
    var listeners: MutableSet<SoundDragListener2> = mutableSetOf()
    var state = IDLE

    /*
    fun stop() {
        isDragged = false
        // Just updates the isDragged LiveData for display purposes
        //viewModel.stopDrag()
    }
     */

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        //return "DraggedSound $hashCode <soundId=${soundId}, isDragged=$isDragged, originalAdapterPosition=$originalAdapterPosition>"
        return "DraggedSound $hashCode <sound=$sound, state=$state, originalAdapterPosition=$originalAdapterPosition, currentAdapterPosition=$currentAdapterPosition>"
    }

    companion object {
        const val IDLE = 0
        const val MOVING = 1
    }
}