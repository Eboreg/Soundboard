package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

abstract class BaseCategoryEditViewModel : ViewModel() {
    abstract val name: LiveData<String?>
    abstract val backgroundColor: LiveData<Int>

    abstract fun setName(value: String): Any?
    abstract fun setBackgroundColor(value: Int): Any?
    abstract fun save(): Any?
}