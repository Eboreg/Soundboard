package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.CategoryWithSounds
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryViewModel(private val category: CategoryWithSounds) : ViewModel() {
    private val colorHelper = ColorHelper(GlobalApplication.application)

    /** Private fields */
    private val repository = CategoryRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).categoryDao())

    /** Model fields */
    val id = category.id

    val order = category.order

    val name = category.name

    val backgroundColor = category.backgroundColor

    val textColor = colorHelper.getTextColorForBackgroundColor(backgroundColor)

    // We are not observing this ATM, just reading it once from DeleteCategoryFragment
    val soundCount = category.soundCount()

    //val sounds = category.sounds

    /** Derived fields */
    //val soundListViewModel = id?.let { id -> SoundListViewModel(id) } ?: SoundListViewModel()

    /** Methods */
    override fun toString() = category.name

    fun delete() = viewModelScope.launch(Dispatchers.IO) { repository.delete(category) }
}