package us.huseli.soundboard.viewmodels

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.CategoryRepository

class CategoryEditViewModel @ViewModelInject constructor(
        private val repository: CategoryRepository,
        @Assisted private val savedStateHandle: SavedStateHandle) : BaseCategoryEditViewModel() {
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

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        category.value?.let {
            _newBackgroundColor.value?.let { color -> it.backgroundColor = color }
            repository.update(it)
        }
    }
}