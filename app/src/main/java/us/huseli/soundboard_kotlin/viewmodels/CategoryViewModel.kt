package us.huseli.soundboard_kotlin.viewmodels

import android.graphics.Color
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryViewModel : ViewModel() {
    private val soundRepository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    private val category = MutableLiveData<Category?>(null)
    private val colorHelper = ColorHelper(GlobalApplication.application)

    fun setCategory(category: Category) {
        this.category.value = category
    }

    fun updateSoundOrder(sounds: List<Sound>) = viewModelScope.launch(Dispatchers.IO) {
        soundRepository.update(sounds)
    }

    val name = category.map { it?.name }

    val backgroundColor = category.map { it?.backgroundColor ?: Color.DKGRAY }

    val textColor = backgroundColor.map { bgc -> colorHelper.getTextColorForBackgroundColor(bgc) }

    val sounds = category.switchMap { soundRepository.getByCategory(it?.id) }

    override fun toString() = category.value?.name ?: ""
}