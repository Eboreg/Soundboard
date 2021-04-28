package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

@Suppress("PropertyName")
abstract class BaseCategoryEditViewModel : ViewModel() {
    protected val _backgroundColor = MutableLiveData<Int>()
    protected val _name = MutableLiveData("")

    abstract val backgroundColor: LiveData<Int>
    val name: LiveData<String>
        get() = _name

    abstract fun save(): Any?

    fun setBackgroundColor(color: Int) {
        _backgroundColor.value = color
    }

    fun setName(value: String) {
        _name.value = value
    }
}