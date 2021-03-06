package us.huseli.soundboard.data

class DraggedSound(
    val soundWithCategory: SoundWithCategory,
    var currentAdapterPosition: Int,
    val viewHeight: Int
) {
    var state = State.IDLE

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "DraggedSound $hashCode <sound=$soundWithCategory, state=$state, currentAdapterPosition=$currentAdapterPosition>"
    }


    enum class State {
        IDLE, MOVING
    }
}