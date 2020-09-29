package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category

class CategoryAddViewModel : BaseCategoryEditViewModel() {
    private val _backgroundColor = MutableLiveData<Int>()
    private val _name = MutableLiveData("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _backgroundColor.postValue(GlobalApplication.colorHelper.randomColor(repository.getUsedColors()))
        }
    }

    override val backgroundColor: LiveData<Int>
        get() = _backgroundColor
    override val name: LiveData<String>
        get() = _name

    override fun setName(value: String) {
        _name.value = value
    }

    override fun setBackgroundColor(value: Int) {
        _backgroundColor.value = value
    }

    // Used by AddCategoryDialogFrament & EditCategoryDialogFrament when saving
    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        // Background colour was initialized from the beginning, so we know it's set
        // And we check for name in BaseCategoryDialogFragment (and set it at init)
        val category = Category(_name.value!!, _backgroundColor.value!!)
        repository.insert(category)
    }
}