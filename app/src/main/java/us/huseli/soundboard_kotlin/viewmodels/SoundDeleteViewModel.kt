package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

class SoundDeleteViewModel : ViewModel() {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    fun delete(soundId: Int) = viewModelScope.launch(Dispatchers.IO) { repository.delete(soundId) }

    fun delete(soundIds: List<Int>?) = viewModelScope.launch(Dispatchers.IO) { repository.delete(soundIds) }
}