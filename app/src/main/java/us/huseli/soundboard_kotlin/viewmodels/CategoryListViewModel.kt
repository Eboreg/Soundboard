package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundDatabase

class CategoryListViewModel : ViewModel() {
    private val repository: CategoryRepository =
            CategoryRepository(SoundDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())
    private val categories = repository.categories
    val categoryViewModels = Transformations.switchMap(categories) {
        MutableLiveData<List<CategoryViewModel>>(it.map { category -> CategoryViewModel(GlobalApplication.application, category) })
    }

    fun save(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        when(category.id) {
            null -> repository.insert(category)
            else -> repository.update(category)
        }
    }

    // Not used?
    fun delete(category: Category) = viewModelScope.launch(Dispatchers.IO) { repository.delete(category) }

    // Will only work if categoryViewModels is observed
    fun get(id: Int): CategoryViewModel? = categoryViewModels.value?.find { it.id.value == id }
}