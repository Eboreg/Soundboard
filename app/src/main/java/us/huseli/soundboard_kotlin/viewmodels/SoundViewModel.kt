package us.huseli.soundboard_kotlin.viewmodels

import android.graphics.Color
import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class SoundViewModel(soundId: Int) : ViewModel() {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())
    private val colorHelper = ColorHelper(GlobalApplication.application)
    private val sound = repository.get(soundId)

    /** Model fields */

    val id = sound.map { it?.id }
    val categoryId = sound.map { it?.categoryId }
    val name = sound.map { it?.name ?: "" }
    val backgroundColor = categoryId.switchMap { catId ->
        catId?.let { id -> repository.getBackgroundColor(id) } ?: liveData { Color.DKGRAY }
    }
    val textColor = backgroundColor.map { colorHelper.getTextColorForBackgroundColor(it) }
    val volume = sound.map { it?.volume ?: 100 }
}