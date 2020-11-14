package us.huseli.soundboard_kotlin.adapters.common

import android.util.Log
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import java.util.*

abstract class DataBoundAdapter<T, VH : DataBoundViewHolder<B, T>, B : ViewDataBinding>(diffCallback: DiffUtil.ItemCallback<T>)
        : LifecycleAdapter<T, VH>(diffCallback) {
    //private val _currentList = mutableListOf<T>()
//    protected open val currentList: List<T>
//        get() = _currentList.toList()

    protected abstract fun createViewHolder(binding: B, parent: ViewGroup): VH

    protected abstract fun createBinding(parent: ViewGroup, viewType: Int): B

    protected abstract fun bind(holder: VH, item: T, position: Int)

    //protected abstract fun calculateDiff(list: List<T>): Any

    //override fun getItemCount() = _currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = createBinding(parent, viewType)
        val holder = createViewHolder(binding, parent)
        binding.lifecycleOwner = holder

        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        super.onBindViewHolder(holder, position)
        //val item = _currentList[position]
        val item = getItem(position)
        Log.i(LOG_TAG, "onBindViewHolder: bind $item to holder=$holder on position=$position")
        bind(holder, item, position)
    }

    fun removeItem(item: T) {
        // "mutations to content must be done through submitList(List)"
        // -- https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter
        val mutableList = currentList.toMutableList()
        mutableList.remove(item)
        submitList(mutableList)
        Log.i(LOG_TAG, "removeItem: removed item $item from $this")

        /*
        val position = currentList.indexOf(item)
        if (position >= 0) {
            // "mutations to content must be done through submitList(List)"
            // -- https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter
            val _currentList = currentList.toMutableList()
            //_currentList.removeAt(position)
            _currentList.remove(item)
            notifyItemRemoved(position)
            Log.i(LOG_TAG, "removeItem: removed item $item from position $position on $this")
        }
         */
    }

    fun removeItemById(id: Int) {
        if (id >= 0) {
            getItemById(id)?.let { item ->
                val mutableList = currentList.toMutableList()
                //val item = mutableList.find { it.id == id }
                mutableList.remove(item)
                submitList(mutableList)
                Log.i(LOG_TAG, "removeItemById: removed $item from position $id on $this")
            }
        }
    }

    abstract fun getItemById(id: Int): T?

    fun moveItem(item: T, toPosition: Int): Boolean {
        /**
         * Returns true if item was in the list, and was successfully moved to toPosition
         * Returns false otherwise
         */
        val fromPosition = currentList.indexOf(item)
        return if (moveItem(fromPosition, toPosition)) {
            Log.i(LOG_TAG, "moveItem: moved item $item from position $fromPosition to position $toPosition")
            true
        } else false
    }

    fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        /**
         * Move item from fromPosition to toPosition
         * Basically returns true if no overflow would occur (both fromPosition and toPosition
         * exist in current list)
         */
        return if (fromPosition in 0 until itemCount && toPosition >= 0 && toPosition < itemCount) {
            val mutableList = currentList.toMutableList()
            if (fromPosition < toPosition)
                for (i in fromPosition until toPosition) Collections.swap(mutableList, i, i + 1)
            else
                for (i in fromPosition downTo toPosition + 1) Collections.swap(mutableList, i, i - 1)
            Log.i(LOG_TAG, "moveItem: moved item from position $fromPosition to position $toPosition")
            submitList(mutableList)
            notifyItemMoved(fromPosition, toPosition)
            true
        } else false
    }

    fun addItem(item: T, toPosition: Int?) {
        // Doesn't care if the item is already in the list (use moveItem() for that)
        val mutableList = currentList.toMutableList()
        if (toPosition == null && toPosition != -1) {
            mutableList.add(item)
            Log.i(LOG_TAG, "addItem: added item $item last in $this")
        } else {
            mutableList.add(toPosition, item)
            Log.i(LOG_TAG, "addItem: added item $item to position $toPosition on $this")
        }
        //notifyItemInserted(toPosition)
        submitList(mutableList)
    }

    // Add item last in list
    fun addItem(item: T) = addItem(item, null)

//    open fun submitList(list: List<T>) {
//        calculateDiff(list)
//        _currentList.clear()
//        _currentList.addAll(list)
//    }


    abstract inner class DiffCallback(private val newRows: List<T>, private val oldRows: List<T>) : DiffUtil.ItemCallback<T>() {
        abstract override fun areItemsTheSame(oldItem: T, newItem: T): Boolean
        abstract override fun areContentsTheSame(oldItem: T, newItem: T): Boolean
    }


    companion object {
        const val LOG_TAG = "DataBoundAdapter"
    }
}