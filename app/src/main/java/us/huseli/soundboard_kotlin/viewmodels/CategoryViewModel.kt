package us.huseli.soundboard_kotlin.viewmodels

import android.graphics.Color
import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryViewModel : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope)
    //private val repository = CategoryRepository(database.categoryDao())
    private val soundRepository = SoundRepository(database.soundDao())

    private val _category = MutableLiveData<Category?>(null)
    private val colorHelper = ColorHelper(GlobalApplication.application)

    val category: LiveData<Category?>
        get() = _category

    fun setCategory(category: Category) {
        _category.value = category
    }

    val id = _category.map { it?.id }

    //val order = _category.map { it?.order }

    val name = _category.map { it?.name }

    val backgroundColor = _category.map { it?.backgroundColor ?: Color.DKGRAY }
    //val backgroundColor = _category.value?.backgroundColor ?: Color.DKGRAY

    val textColor = backgroundColor.map { bgc -> colorHelper.getTextColorForBackgroundColor(bgc) }
    //val textColor = colorHelper.getTextColorForBackgroundColor(backgroundColor)

    val sounds = id.switchMap { soundRepository.getByCategory(it) }

    override fun toString() = _category.value?.name ?: ""
}