package us.huseli.soundboard.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel2 @Inject constructor(
    private val repository: CategoryRepository,
    private val soundRepository: SoundRepository,
    private val undoRepository: UndoRepository,
) : ViewModel() {
    val categoryId = MutableLiveData<Int?>(null)

    val sounds = Transformations.switchMap(categoryId) { id ->
        id?.let { soundRepository.listExtendedByCategory(id).asLiveData() }
    }

    fun setCollapsed(value: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        categoryId.value?.let { repository.setCollapsed(it, value) }
    }

    fun expand() = setCollapsed(false)

    fun collapse() = setCollapsed(true)

    fun swap(oldPos: Int, newPos: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.swap(oldPos, newPos)
        undoRepository.pushState()
    }
}