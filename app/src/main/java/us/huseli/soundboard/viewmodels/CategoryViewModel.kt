package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import us.huseli.soundboard.helpers.ColorHelper
import us.huseli.soundboard.helpers.Functions
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: CategoryRepository,
    private val soundRepository: SoundRepository,
    private val undoRepository: UndoRepository,
    private val colorHelper: ColorHelper
) : ViewModel() {
    private val emptyCategory = Category(Functions.umlautify(context.getString(R.string.unchanged)).toString())

    val categories = repository.categories.map { list ->
        list.forEach {
            it.textColor = colorHelper.getColorOnBackground(it.backgroundColor)
        }
        list
    }
    val categoriesWithEmpty = categories.map { listOf(emptyCategory) + it }

    fun listSounds(categoryId: Int?) = soundRepository.listExtended().map { list ->
        list.filter { it.categoryId == categoryId }
    }.asLiveData()

    fun setCollapsed(categoryId: Int, value: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.setCollapsed(categoryId, value)
    }

    fun expand(categoryId: Int?) {
        if (categoryId != null) setCollapsed(categoryId, false)
    }

    fun collapse(categoryId: Int?) {
        if (categoryId != null) setCollapsed(categoryId, true)
    }

    fun create(name: CharSequence) = viewModelScope.launch(Dispatchers.IO) {
        /** Used in MainActivity.onCreate() to create empty default category if there are none */
        repository.insert(Category(name.toString(), colorHelper.getRandomColor()))
    }

    /** Used by DeleteCategoryFragment */
    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        soundRepository.deleteByCategory(id)
        repository.delete(id)
        undoRepository.pushState()
    }

    fun swap(oldPos: Int, newPos: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.swap(oldPos, newPos)
        undoRepository.pushState()
    }
}