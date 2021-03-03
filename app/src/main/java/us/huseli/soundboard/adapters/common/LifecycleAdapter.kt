package us.huseli.soundboard.adapters.common

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class LifecycleAdapter<T, VH : LifecycleViewHolder<T>>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diffCallback) {
    @Suppress("PrivatePropertyName")
    private val LOG_TAG = "LifecycleAdapter"
    protected val viewHolders = mutableListOf<VH>()

    open val firstVisibleViewHolder: VH?
        get() = viewHolders.firstOrNull { it.isVisible() }
    val firstVisibleItem: T?
        get() = firstVisibleViewHolder?.item

    open val lastVisibleViewHolder: VH?
        get() = viewHolders.lastOrNull { it.isVisible() }
    val lastVisibleItem: T?
        get() = lastVisibleViewHolder?.item

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.markCreated()
        viewHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        // Log.d(LOG_TAG, "onViewDetachedFromWindow: holder=$holder")
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    override fun onViewAttachedToWindow(holder: VH) {
        // Log.d(LOG_TAG, "onViewAttachedToWindow: holder=$holder")
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    fun setLifecycleDestroyed() {
        // Log.d(LOG_TAG, "setLifecycleDestroyed")
        viewHolders.forEach { it.markDestroyed() }
        viewHolders.clear()
    }

}