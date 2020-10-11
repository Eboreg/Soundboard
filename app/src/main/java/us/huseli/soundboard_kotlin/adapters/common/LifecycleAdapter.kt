package us.huseli.soundboard_kotlin.adapters.common

import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView

abstract class LifecycleAdapter<VH: LifecycleViewHolder> : RecyclerView.Adapter<VH>() {
    protected val viewHolders = mutableListOf<VH>()

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        holder.markCreated()
        viewHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    fun setLifecycleDestroyed() {
        viewHolders.forEach {
            it.markDestroyed()
        }
    }
}