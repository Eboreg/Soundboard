package us.huseli.soundboard_kotlin.data

class DraggedSound(val sound: Sound, private val originalAdapterPosition: Int) {
    var currentAdapterPosition: Int = originalAdapterPosition
    var state = State.IDLE

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "DraggedSound $hashCode <sound=$sound, state=$state, originalAdapterPosition=$originalAdapterPosition, currentAdapterPosition=$currentAdapterPosition>"
    }


    enum class State {
        IDLE, MOVING
    }
}