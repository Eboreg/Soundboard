package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryAddViewModel : BaseCategoryEditViewModel() {
    private val colorHelper = ColorHelper(GlobalApplication.application)
    private val _backgroundColor = MutableLiveData(colorHelper.colors.random())
    private val _name = MutableLiveData("")

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
        // We checked for empty category name in BaseCategoryDialogFragment, so we know it exists
        // and is not empty
        // And background colour was initialized from the beginning, so we know it's set
        val category = Category(_name.value!!, _backgroundColor.value!!)
        repository.insert(category)
    }
}