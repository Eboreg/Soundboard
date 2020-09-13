package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryListViewModel : ViewModel() {
    private val repository = CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())
    private val colorHelper = ColorHelper(GlobalApplication.application)
    val categories = repository.categories

    fun saveOrder(categories: List<Category>) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveOrder(categories)
    }

    fun create(name: String) = viewModelScope.launch(Dispatchers.IO) { repository.insert(Category(name, colorHelper.colors.random())) }

    // Used by DeleteCategoryFragment
    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) { repository.delete(id) }

    fun getCategoryEditViewModel(id: Int) = CategoryEditViewModel(id)
}