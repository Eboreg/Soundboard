package us.huseli.soundboard_kotlin.adapters.common

import android.util.Log
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil

abstract class DataBoundAdapter<T, VH : DataBoundViewHolder<B, T>, B : ViewDataBinding>(diffCallback: DiffUtil.ItemCallback<T>)
        : LifecycleAdapter<T, VH>(diffCallback) {
    override val LOG_TAG = "DataBoundAdapter"

    protected abstract fun createViewHolder(binding: B, parent: ViewGroup): VH

    protected abstract fun createBinding(parent: ViewGroup, viewType: Int): B

    protected abstract fun bind(holder: VH, item: T, position: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = createBinding(parent, viewType)
        val holder = createViewHolder(binding, parent)
        binding.lifecycleOwner = holder

        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position)
        Log.i(LOG_TAG, "onBindViewHolder: bind $item to holder=$holder on position=$position ----- adapter=$this")
        bind(holder, item, position)
    }

    fun removeItem(item: T) {
        // TODO: Not used? How are items being removed?
        // "mutations to content must be done through submitList(List)"
        // -- https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter
        val mutableList = currentList.toMutableList()
        mutableList.remove(item)
        submitList(mutableList)
        Log.i(LOG_TAG, "removeItem: removed item $item from $this")
    }

    fun removeItemById(id: Int) {
        // TODO: Not used? How are items being removed?
        if (id >= 0) {
            getItemById(id)?.let { item ->
                val mutableList = currentList.toMutableList()
                mutableList.remove(item)
                submitList(mutableList)
                Log.i(LOG_TAG, "removeItemById: removed $item from position $id on $this")
            }
        }
    }

    abstract fun getItemById(id: Int): T?


    abstract inner class DiffCallback : DiffUtil.ItemCallback<T>() {
        abstract override fun areItemsTheSame(oldItem: T, newItem: T): Boolean
        abstract override fun areContentsTheSame(oldItem: T, newItem: T): Boolean
    }
}