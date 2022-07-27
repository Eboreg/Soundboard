package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

@Suppress("PropertyName")
abstract class BaseCategoryEditViewModel : ViewModel() {
    protected val _backgroundColor = MutableLiveData<Int>()
    protected val _name = MutableLiveData("")

    open val backgroundColor: LiveData<Int>
        get() = _backgroundColor
    fun setBackgroundColor(color: Int) {
        _backgroundColor.value = color
    }

    val name: LiveData<String>
        get() = _name
    fun setName(value: String) {
        _name.value = value
    }

    abstract fun save(): Any?
}