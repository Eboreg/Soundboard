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
import java.util.*

class CategoryListViewModel : ViewModel() {
    private val repository: CategoryRepository =
            CategoryRepository(SoundDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())
    private val categories = repository.categories
    val categoryViewModels = Transformations.switchMap(categories) {
        MutableLiveData(it.map { category -> CategoryViewModel(GlobalApplication.application, category, category.category.order) })
    }

    fun updateOrder(fromPosition: Int, toPosition: Int) = viewModelScope.launch(Dispatchers.IO) {
        categories.value?.let {
            if (fromPosition < toPosition)
                for (i in fromPosition until toPosition) Collections.swap(it, i, i + 1)
            else
                for (i in fromPosition downTo toPosition + 1) Collections.swap(it, i, i - 1)
            it.forEachIndexed { idx, c ->
                if (c.category.order != idx) {
                    c.category.order = idx
                    repository.update(c.category)
                }
            }
        }
    }

    // Not used?
    fun delete(category: Category) = viewModelScope.launch(Dispatchers.IO) { repository.delete(category) }

    // Will only work if categoryViewModels is observed
    fun get(id: Int): CategoryViewModel? = categoryViewModels.value?.find { it.id == id }
}