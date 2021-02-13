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

    fun markCreated() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun markAttach() {
        if (wasPaused) {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            wasPaused = false
        } else
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun markDetach() {
        wasPaused = true
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun markDestroyed() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle() = lifecycleRegistry
}