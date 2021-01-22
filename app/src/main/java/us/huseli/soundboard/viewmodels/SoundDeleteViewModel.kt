package us.huseli.soundboard.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.SoundRepository

class SoundDeleteViewModel @ViewModelInject constructor(private val repository: SoundRepository) : ViewModel() {
    fun delete(soundId: Int) = delete(listOf(soundId))

    fun delete(soundIds: List<Int>?) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(soundIds)
    }
}