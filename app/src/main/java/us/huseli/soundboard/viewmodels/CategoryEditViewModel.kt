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

    private var category: Category? = null

    override val backgroundColor: LiveData<Int>
        get() = _backgroundColor

    var sortOrder = SoundSorting.Order.ASCENDING
    var sortParameter: SoundSorting.Parameter? = null

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        repository.update(category, name.value, backgroundColor.value)
        sortParameter?.also {
            if (it != SoundSorting.Parameter.UNDEFINED) soundRepository.sort(category, SoundSorting(it, sortOrder))
        }
        undoRepository.pushState()
    }

    fun setup(category: Category) {
        this.category = category
        setName(category.name)
        setBackgroundColor(category.backgroundColor)
    }
}