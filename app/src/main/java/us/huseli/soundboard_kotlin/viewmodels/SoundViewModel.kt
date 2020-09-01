package us.huseli.soundboard_kotlin.viewmodels

import android.graphics.Color
import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

@Suppress("LocalVariableName")
class SoundViewModel(soundId: Int) : ViewModel() {
//class SoundViewModel(private val sound: Sound) : ViewModel() {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())
    private val _sound = repository.get(soundId)

    // TODO: This has to fetch sound from repository, not receive it from outside
    // Or maybe take a LiveData as parameter?
    // Otherwise properties don't get auto-updated

    /** Model fields */
    val id = _sound.map { it.id }
    val categoryId = _sound.map { it.categoryId }
    val name = _sound.map { it.name }
    val backgroundColor = _sound.switchMap { sound ->
        sound.id?.let { id -> repository.getBackgroundColor(id) } ?: liveData { Color.DKGRAY }
    }
    val textColor = backgroundColor.map { ColorHelper(GlobalApplication.application).getTextColorForBackgroundColor(it) }
    val volume = _sound.map { it.volume }

    //val textColor = ColorHelper(GlobalApplication.application).getTextColorForBackgroundColor(backgroundColor)

}