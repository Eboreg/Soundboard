package us.huseli.soundboard_kotlin.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.CategoryExtended
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryViewModel(application: Application, category: CategoryExtended) : AndroidViewModel(application) {
    private val colorHelper = ColorHelper(application)

    /** Private fields */
    private val repository = CategoryRepository(SoundboardDatabase.getInstance(application, viewModelScope).categoryDao())
    private val _categoryWithSounds = category

    /** Model fields */
    val id = _categoryWithSounds.category.id

    val order = _categoryWithSounds.category.order

    val name = _categoryWithSounds.category.name

    val backgroundColor = _categoryWithSounds.category.backgroundColor

    val textColor = colorHelper.getTextColorForBackgroundColor(backgroundColor)

    // We are not observing this ATM, just reading it once from DeleteCategoryFragment
    val soundCount = _categoryWithSounds.soundCount

    /** Derived fields */
    val soundListViewModel = id?.let { id -> SoundListViewModel(id) } ?: SoundListViewModel()

    /** Methods */
    override fun toString() = _categoryWithSounds.category.name

    fun delete() = viewModelScope.launch(Dispatchers.IO) { repository.delete(_categoryWithSounds.category) }
}