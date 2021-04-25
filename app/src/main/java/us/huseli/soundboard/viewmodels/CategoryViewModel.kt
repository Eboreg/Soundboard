package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import us.huseli.soundboard.helpers.ColorHelper
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: CategoryRepository,
    private val soundRepository: SoundRepository,
    private val undoRepository: UndoRepository,
    private val colorHelper: ColorHelper
) : ViewModel() {
    private val emptyCategory = Category(context.getString(R.string.unchanged))

    val categories = repository.categories.map { list ->
        list.forEach {
            it.textColor = colorHelper.getColorOnBackground(it.backgroundColor)
        }
        list
    }
    val categoriesWithEmpty = categories.map { listOf(emptyCategory) + it }

    fun setCollapsed(categoryId: Int, value: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.setCollapsed(categoryId, value)
    }

    fun expand(category: Category) = category.id?.let { setCollapsed(it, false) }

    fun collapse(category: Category) = category.id?.let { setCollapsed(it, true) }

    fun create(name: String) = viewModelScope.launch(Dispatchers.IO) {
        /** Used in MainActivity.onCreate() to create empty default category if there are none */
        repository.insert(Category(name, colorHelper.getRandomColor()))
    }

    /** Used by DeleteCategoryFragment */
    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        soundRepository.deleteByCategory(id)
        repository.delete(id)
        undoRepository.pushState()
    }

    fun switch(oldPos: Int, newPos: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.switch(oldPos, newPos)
        undoRepository.pushState()
    }
}