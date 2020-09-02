package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

class CategoryListViewModel : ViewModel() {
    private val repository: CategoryRepository =
            CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())
    val categories = repository.categories

    fun updateOrder(categories: List<Category>) = viewModelScope.launch(Dispatchers.IO) {
        // Set category.order according to the order in the received list
        repository.updateOrder(categories)
    }

    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) { repository.delete(id) }

    fun getCategoryEditViewModel(id: Int) = CategoryEditViewModel(id)
}