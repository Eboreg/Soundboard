package us.huseli.soundboard_kotlin.adapters.common

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class LifecycleAdapter<T, VH: LifecycleViewHolder>(diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback) {
    @Suppress("PrivatePropertyName")
    private val LOG_TAG = "LifecycleAdapter"

    private val viewHolders = mutableListOf<VH>()

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.markCreated()
        viewHolders.add(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        Log.i(LOG_TAG, "onViewDetachedFromWindow: holder=$holder")
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    override fun onViewAttachedToWindow(holder: VH) {
        Log.i(LOG_TAG, "onViewAttachedToWindow: holder=$holder")
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    fun setLifecycleDestroyed() {
        Log.i(LOG_TAG, "setLifecycleDestroyed")
        viewHolders.forEach { it.markDestroyed() }
        viewHolders.clear()
    }
}