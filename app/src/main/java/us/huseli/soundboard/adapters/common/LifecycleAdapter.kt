package us.huseli.soundboard.adapters.common

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class LifecycleAdapter<T, VH : LifecycleViewHolder<T>>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diffCallback) {
    @Suppress("PrivatePropertyName")
    private val LOG_TAG = "LifecycleAdapter"
    private val viewHolders = mutableListOf<VH>()

    val firstVisibleViewHolder: VH?
        get() {
            val location = IntArray(2)
            return viewHolders.firstOrNull {
                it.itemView.getLocationOnScreen(location)
                location[1] != 0 && location[1] + it.itemView.height >= 0
            }
        }
    val firstVisibleItem: T?
        get() = firstVisibleViewHolder?.item

    val lastVisibleViewHolder: VH?
        get() {
            val location = IntArray(2)
            return viewHolders.lastOrNull {
                it.itemView.getLocationOnScreen(location)
                location[1] != 0 && location[1] <= it.itemView.context.resources.displayMetrics.heightPixels
            }
        }
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