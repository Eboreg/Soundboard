package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

abstract class BaseCategoryEditViewModel : ViewModel() {
    internal val repository = CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application).categoryDao())

    abstract val name: LiveData<String>
    abstract val backgroundColor: LiveData<Int>

    abstract fun setName(value: String): Any?
    abstract fun setBackgroundColor(value: Int): Any?
    abstract fun save(): Any?
}