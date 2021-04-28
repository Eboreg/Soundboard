package us.huseli.soundboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject

@HiltViewModel
class SoundDeleteViewModel @Inject constructor(
    private val repository: SoundRepository, private val undoRepository: UndoRepository) : ViewModel() {

    fun delete(soundIds: List<Int>?) {
        soundIds?.also {
            viewModelScope.launch(Dispatchers.IO) {
                repository.delete(soundIds)
                undoRepository.pushState()
            }
        }
    }
}