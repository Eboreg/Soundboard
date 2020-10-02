package us.huseli.soundboard_kotlin.adapters.common

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil

abstract class DataBoundAdapter<T, VH: DataBoundViewHolder<B>, B: ViewDataBinding> : LifecycleAdapter<VH>() {
    private val viewHolders = mutableListOf<VH>()
    internal open val currentList = mutableListOf<T>()

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

    protected abstract fun bind(holder: VH, item: T, position: Int)

    protected abstract fun calculateDiff(list: List<T>): Any

    override fun getItemCount() = currentList.size

    override fun onBindViewHolder(holder: VH, position: Int) = bind(holder, currentList[position], position)

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.markDetach()
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        holder.markAttach()
    }

    open fun submitList(list: List<T>) {
        calculateDiff(list)
        currentList.clear()
        currentList.addAll(list)
    }

    fun getViewHolder(position: Int) = viewHolders[position]


    abstract inner class DiffCallback(private val newRows: List<T>, private val oldRows: List<T>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldRows.size
        override fun getNewListSize() = newRows.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                = areItemsTheSame(oldRows[oldItemPosition], newRows[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                = areContentsTheSame(oldRows[oldItemPosition], newRows[newItemPosition])

        abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean
        abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean
    }
}