package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CategoryRepository
    val categories: LiveData<List<Category>>

    init {
        val dao = SoundDatabase.getInstance(application, viewModelScope).categoryDao()
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