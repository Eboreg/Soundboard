package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import us.huseli.soundboard_kotlin.data.Category

class CategoryViewModelFactory(internal val category: Category) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Category::class.java).newInstance(category)
    }
}