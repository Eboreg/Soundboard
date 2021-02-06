package us.huseli.soundboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.SoundRepository
import javax.inject.Inject

@HiltViewModel
class SoundDeleteViewModel @Inject constructor(private val repository: SoundRepository) :
    ViewModel() {
    fun delete(soundId: Int) = delete(listOf(soundId))

    fun delete(soundIds: List<Int>?) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(soundIds)
    }
}