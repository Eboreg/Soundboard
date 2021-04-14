package us.huseli.soundboard.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val undoRepository: UndoRepository,
    private val soundRepository: SoundRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseCategoryEditViewModel() {

    private val category = savedStateHandle.getLiveData<Int>("categoryId").switchMap { categoryId ->
        repository.get(categoryId)
    }
    private val _newBackgroundColor = MutableLiveData<Int>()
    private val _originalBackgroundColor = category.map { it?.backgroundColor }
    private val _backgroundColor = MediatorLiveData<Int>()

    init {
        _backgroundColor.addSource(_originalBackgroundColor) { _backgroundColor.value = it }
        _backgroundColor.addSource(_newBackgroundColor) { _backgroundColor.value = it }
    }

    override var name: LiveData<String?> = category.map { it?.name }
    override val backgroundColor: LiveData<Int>
        get() = _backgroundColor

    fun setCategoryId(id: Int) {
        savedStateHandle["categoryId"] = id
    }

    override fun setName(value: String) {
        category.value?.name = value
    }

    override fun setBackgroundColor(value: Int) {
        _newBackgroundColor.value = value
    }

    override fun save(soundSorting: Sound.Sorting?) = viewModelScope.launch(Dispatchers.IO) {
        category.value?.let {
            _newBackgroundColor.value?.let { color -> it.backgroundColor = color }
            repository.update(it)
            if (soundSorting != null) soundRepository.sort(it.id, soundSorting)
            undoRepository.pushState()
        }
    }
}