package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

abstract class BaseCategoryEditViewModel : ViewModel() {
    internal val repository = CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())

    abstract val name: LiveData<String>
    abstract val backgroundColor: LiveData<Int>

    abstract fun setName(value: String)
    abstract fun setBackgroundColor(value: Int)
    abstract fun save(): Job
}