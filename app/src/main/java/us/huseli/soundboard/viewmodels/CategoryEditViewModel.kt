package us.huseli.soundboard.viewmodels

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryEditViewModel(categoryId: Int) : BaseCategoryEditViewModel() {
    private val category = repository.get(categoryId)
    private val _newBackgroundColor = MutableLiveData<Int>()
    private val _originalBackgroundColor = category.map { it.backgroundColor }
    private val _backgroundColor = MediatorLiveData<Int>()

    init {
        _backgroundColor.addSource(_originalBackgroundColor) { _backgroundColor.value = it }
        _backgroundColor.addSource(_newBackgroundColor) { _backgroundColor.value = it }
    }

    override var name = category.map { it.name }
    override val backgroundColor: LiveData<Int>
        get() = _backgroundColor

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