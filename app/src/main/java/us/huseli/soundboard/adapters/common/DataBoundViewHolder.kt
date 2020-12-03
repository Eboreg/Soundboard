package us.huseli.soundboard.adapters.common

import androidx.databinding.ViewDataBinding

abstract class DataBoundViewHolder<B : ViewDataBinding, T>(internal open val binding: B) : LifecycleViewHolder(binding.root)