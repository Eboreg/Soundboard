package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryEditViewModel(private val category: Category?, private val order: Int?) : ViewModel() {
    private val repository: CategoryRepository =
            CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())
    private val colorHelper = ColorHelper(GlobalApplication.application)
    private val _backgroundColor = MutableLiveData(category?.backgroundColor ?: colorHelper.colors.random())

    var name = category?.name ?: ""
    val backgroundColor: LiveData<Int>
        get() = _backgroundColor

    fun setBackgroundColor(value: Int) {
        _backgroundColor.value = value
    }

    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            @Suppress("LocalVariableName")
            val _category = category?.also { category ->
                category.name = name
                category.backgroundColor = _backgroundColor.value!!
            } ?: Category(name, _backgroundColor.value!!, order ?: 0)
            when (_category.id) {
                null -> repository.insert(_category)
                else -> repository.update(_category)
            }
        }
    }
}