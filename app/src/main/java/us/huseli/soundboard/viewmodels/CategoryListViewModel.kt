package us.huseli.soundboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.SoundboardDatabase

class CategoryListViewModel : ViewModel() {
    private val repository = CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application).categoryDao())
    private val emptyCategory = Category("(Unchanged)")

    val categories = repository.categories
    val categoriesWithEmpty = categories.map { listOf(emptyCategory) + it }

    fun create(name: String) = viewModelScope.launch(Dispatchers.IO) {
        /** Used in MainActivity.onCreate() to create empty default category if there are none */
        repository.insert(Category(name, GlobalApplication.application.getColorHelper().randomColor(repository.getUsedColors())))
    }

    /** Used by DeleteCategoryFragment */
    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) { repository.delete(id) }

    fun getCategoryEditViewModel(id: Int) = CategoryEditViewModel(id)

    fun saveOrder(categories: List<Category>) = viewModelScope.launch(Dispatchers.IO) {
        /** Save .order of all categories as set right now */
        repository.saveOrder(categories)
    }
}