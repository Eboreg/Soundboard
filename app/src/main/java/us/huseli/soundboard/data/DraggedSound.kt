package us.huseli.soundboard.data

class DraggedSound(
    val soundId: Int,
    var currentAdapterPosition: Int,
    val viewHeight: Int
) {
    var state = State.IDLE

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "DraggedSound $hashCode <soundId=$soundId, state=$state, currentAdapterPosition=$currentAdapterPosition>"
    }


    enum class State {
        IDLE, MOVING
    }
}