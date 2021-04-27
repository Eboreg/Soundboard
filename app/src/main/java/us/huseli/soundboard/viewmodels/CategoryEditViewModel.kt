package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.*
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val undoRepository: UndoRepository,
    private val soundRepository: SoundRepository
) : BaseCategoryEditViewModel() {

    private var categoryId: Int? = null

    override val backgroundColor: LiveData<Int>
        get() = _backgroundColor

    var sortOrder = SoundSorting.Order.ASCENDING
    var sortParameter: SoundSorting.Parameter? = null

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        repository.update(categoryId, name.value, backgroundColor.value)
        sortParameter?.also {
            if (it != SoundSorting.Parameter.UNDEFINED) soundRepository.sort(categoryId, SoundSorting(it, sortOrder))
        }
        undoRepository.pushState()
    }

    fun setup(category: Category) {
        categoryId = category.id
        setName(category.name)
        setBackgroundColor(category.backgroundColor)
    }
}