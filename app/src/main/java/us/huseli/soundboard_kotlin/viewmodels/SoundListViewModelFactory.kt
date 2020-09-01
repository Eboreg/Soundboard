package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class SoundListViewModelFactory(private val categoryId: Int?) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoundListViewModel::class.java))
            return SoundListViewModel(categoryId) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}