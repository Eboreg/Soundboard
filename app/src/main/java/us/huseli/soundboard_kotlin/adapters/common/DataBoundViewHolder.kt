package us.huseli.soundboard_kotlin.adapters.common

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView

open class DataBoundViewHolder<B: ViewDataBinding>(internal val binding: B) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {
    @Suppress("LeakingThis")
    private val lifecycleRegistry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.INITIALIZED }
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

    fun markDestroyed() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle() = lifecycleRegistry
}