package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication

class CategoryListViewModel(private val state: SavedStateHandle) : ViewModel() {
    private val repository: CategoryRepository
    val categories: LiveData<List<Category>>

    init {
        val dao = SoundDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao()
        repository = CategoryRepository(dao)
        categories = repository.categories
    }

    fun save(category: Category) = viewModelScope.launch(Dispatchers.IO) {
        when(category.id) {
            null -> repository.insert(category)
            else -> repository.update(category)
        }
    }

    fun get(id: Int): Category? = repository.get(id)
}