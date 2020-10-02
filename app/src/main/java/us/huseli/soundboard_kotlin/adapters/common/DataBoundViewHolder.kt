package us.huseli.soundboard_kotlin.adapters.common

import androidx.databinding.ViewDataBinding

abstract class DataBoundViewHolder<B: ViewDataBinding>(internal val binding: B) : LifecycleViewHolder(binding.root)