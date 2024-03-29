package us.huseli.soundboard.adapters.common

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class LifecycleAdapter<T, VH : LifecycleViewHolder<T>>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diffCallback) {

    protected val viewHolders = mutableListOf<VH>()

    override fun onBindViewHolder(holder: VH, position: Int) {
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
        viewHolders.forEach { it.markDestroyed() }
        viewHolders.clear()
    }

}