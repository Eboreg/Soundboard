package us.huseli.soundboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase

class SoundDeleteViewModel : ViewModel() {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application).soundDao())

    fun delete(soundId: Int) = delete(listOf(soundId))

    fun delete(soundIds: List<Int>?) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(soundIds)
    }
}