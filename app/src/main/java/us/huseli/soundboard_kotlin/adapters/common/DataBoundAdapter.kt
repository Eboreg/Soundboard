package us.huseli.soundboard_kotlin.adapters.common

import android.util.Log
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import java.util.*

abstract class DataBoundAdapter<T, VH : DataBoundViewHolder<B, T>, B : ViewDataBinding> : LifecycleAdapter<VH>() {
    private val _currentList = mutableListOf<T>()
    protected open val currentList: List<T>
        get() = _currentList.toList()

    protected abstract fun createViewHolder(binding: B, parent: ViewGroup): VH

    protected abstract fun createBinding(parent: ViewGroup, viewType: Int): B

    protected abstract fun bind(holder: VH, item: T, position: Int)

    protected abstract fun calculateDiff(list: List<T>): Any

    override fun getItemCount() = _currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = createBinding(parent, viewType)
        val holder = createViewHolder(binding, parent)
        binding.lifecycleOwner = holder

        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = _currentList[position]
        Log.i(LOG_TAG, "onBindViewHolder: bind $item to holder=$holder on position=$position")
        bind(holder, item, position)
    }

    fun removeItem(item: T) {
        val position = _currentList.indexOf(item)
        if (position >= 0) {
            _currentList.removeAt(position)
            notifyItemRemoved(position)
            Log.i(LOG_TAG, "removeItem: removed item $item from position $position on $this")
        }
    }

    // Returns true if item was in the list, and was moved to toPosition
    // Returns false if item was not in the list or is null
    fun moveItem(item: T, toPosition: Int): Boolean {
        val fromPosition = _currentList.indexOf(item)
        return if (fromPosition > -1) {
            if (fromPosition < toPosition)
                for (i in fromPosition until toPosition) Collections.swap(_currentList, i, i + 1)
            else
                for (i in fromPosition downTo toPosition + 1) Collections.swap(_currentList, i, i - 1)
            Log.i(LOG_TAG, "moveItem: moved item $item from position $fromPosition to position $toPosition")
            notifyItemMoved(fromPosition, toPosition)
            true
        } else false
    }

    // Doesn't care if the item is already in the list (use moveItem() for that)
    fun addItem(item: T, position: Int?) {
        val toPosition = if (position == null) {
            _currentList.add(item)
            _currentList.indexOf(item)
        } else {
            _currentList.add(position, item)
            position
        }
        notifyItemInserted(toPosition)
        Log.i(LOG_TAG, "addItem: added item $item to position $toPosition on $this")
    }

    open fun submitList(list: List<T>) {
        calculateDiff(list)
        _currentList.clear()
        _currentList.addAll(list)
    }


    abstract inner class DiffCallback(private val newRows: List<T>, private val oldRows: List<T>) : DiffUtil.Callback() {
        override fun getOldListSize() = oldRows.size
        override fun getNewListSize() = newRows.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = areItemsTheSame(oldRows[oldItemPosition], newRows[newItemPosition])

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = areContentsTheSame(oldRows[oldItemPosition], newRows[newItemPosition])

        abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean
        abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean
    }


    companion object {
        const val LOG_TAG = "DataBoundAdapter"
    }
}