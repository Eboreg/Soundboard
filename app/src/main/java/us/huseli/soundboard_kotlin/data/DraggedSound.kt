package us.huseli.soundboard_kotlin.data

class DraggedSound(val soundId: Int, var originalAdapterPosition: Int) {
    var isDragged = true
    var state = IDLE

    fun stop() {
        isDragged = false
        // Just updates the isDragged LiveData for display purposes
        //viewModel.stopDrag()
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "DraggedSound $hashCode <soundId=${soundId}, isDragged=$isDragged, originalAdapterPosition=$originalAdapterPosition>"
    }

    companion object {
        const val IDLE = 0
        const val MOVING = 1
    }
}