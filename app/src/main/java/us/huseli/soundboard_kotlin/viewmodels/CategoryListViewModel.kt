package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.CategoryWithSounds
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

class CategoryListViewModel : ViewModel() {
    private val repository: CategoryRepository =
            CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())
    val categories = repository.categoryWithSounds
/*
    private val categoryViewModels = Transformations.switchMap(categories) {
        MutableLiveData(it.map { category -> CategoryViewModel(category) })
    }
*/

    val maxOrder = Transformations.switchMap(categories) { list ->
        liveData<Int?> { list.maxByOrNull { it.order } }
    }

    fun updateOrder(categories: List<CategoryWithSounds>) = viewModelScope.launch(Dispatchers.IO) {
        // Set category.order according to the order in the received list
        repository.updateOrder(categories)
    }

    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) { repository.delete(id) }

    // Will only work if categoryViewModels is observed
    // fun getCategoryViewModel(id: Int): CategoryViewModel? = categoryViewModels.value?.find { it.id == id }

    fun getCategoryEditViewModel(id: Int): CategoryEditViewModel? {
        return categories.value?.find { it.id == id }?.let { CategoryEditViewModel(it, it.order) }
    }
}