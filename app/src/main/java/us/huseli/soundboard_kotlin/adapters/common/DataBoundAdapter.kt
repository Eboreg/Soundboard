package us.huseli.soundboard_kotlin.adapters.common

import android.util.Log
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil

abstract class DataBoundAdapter<T, VH : DataBoundViewHolder<B, T>, B : ViewDataBinding>(diffCallback: DiffUtil.ItemCallback<T>)
        : LifecycleAdapter<T, VH>(diffCallback) {
    @Suppress("PrivatePropertyName")
    private val LOG_TAG = "DataBoundAdapter"

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

    abstract fun getItemById(id: Int): T?


    abstract inner class DiffCallback : DiffUtil.ItemCallback<T>() {
        abstract override fun areItemsTheSame(oldItem: T, newItem: T): Boolean
        abstract override fun areContentsTheSame(oldItem: T, newItem: T): Boolean
    }
}