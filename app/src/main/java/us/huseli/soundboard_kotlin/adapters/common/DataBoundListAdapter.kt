package us.huseli.soundboard_kotlin.adapters.common

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class DataBoundListAdapter<T, VH: DataBoundViewHolder<B>, B: ViewDataBinding>(diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback) {
//abstract class DataBoundListAdapter<T, VH: DataBoundViewHolder<B>, B: ViewDataBinding>(config: AsyncDifferConfig<T>) : ListAdapter<T, VH>(config) {
    private val viewHolders = mutableListOf<VH>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = createBinding(parent, viewType)
        val viewHolder = createViewHolder(binding, parent)
        viewHolder.lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        binding.lifecycleOwner = viewHolder
        viewHolder.markCreated()
        viewHolders.add(viewHolder)

        return viewHolder
    }

    protected abstract fun createViewHolder(binding: B, parent: ViewGroup): VH

    protected abstract fun createBinding(parent: ViewGroup, viewType: Int): B

    override fun onBindViewHolder(holder: VH, position: Int) {
        bind(holder, getItem(position))
/*
        if (position < itemCount) {
            bind(holder, getItem(position))
            holder.binding.executePendingBindings()
        }
*/
    }

    protected abstract fun bind(holder: VH, item: T)

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