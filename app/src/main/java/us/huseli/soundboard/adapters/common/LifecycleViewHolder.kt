package us.huseli.soundboard.adapters.common

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView

abstract class LifecycleViewHolder<T>(view: View) : RecyclerView.ViewHolder(view), LifecycleOwner {
    internal abstract val lifecycleRegistry: LifecycleRegistry
    internal abstract var item: T?

    private var wasPaused = false

    // TODO: Not used ATM, maybe in the future
    fun isVisible(): Boolean {
        val location = IntArray(2)
        itemView.getLocationOnScreen(location)
        return location[1] != 0 && location[1] + itemView.height >= 0 && location[1] <= itemView.context.resources.displayMetrics.heightPixels
    }

    open fun markCreated() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun markAttach() {
        if (wasPaused) {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            wasPaused = false
        } else
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    open fun markDetach() {
        wasPaused = true
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun markDestroyed() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle() = lifecycleRegistry
}