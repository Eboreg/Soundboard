package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CategoryViewModelFactory(internal val categoryId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Int::class.java).newInstance(categoryId)
    }
}