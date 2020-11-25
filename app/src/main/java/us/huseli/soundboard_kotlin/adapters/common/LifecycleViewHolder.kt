package us.huseli.soundboard_kotlin.adapters.common

import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView

abstract class LifecycleViewHolder(view: View) : RecyclerView.ViewHolder(view), LifecycleOwner {
    @Suppress("PropertyName")
    open val LOG_TAG = "LifecycleViewHolder"
    internal abstract val lifecycleRegistry: LifecycleRegistry

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
        Log.d(LOG_TAG, "markDetach: $this")
        wasPaused = true
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun markDestroyed() {
        Log.i(LOG_TAG, "markDestroyed: $this")
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle() = lifecycleRegistry

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "LifecycleViewHolder $hashCode <itemId=$itemId, itemView=$itemView, adapterPosition=$adapterPosition>"
    }
}