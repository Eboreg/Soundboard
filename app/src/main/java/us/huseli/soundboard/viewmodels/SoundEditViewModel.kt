package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject

@HiltViewModel
class SoundEditViewModel @Inject constructor(
    private val repository: SoundRepository, private val undoRepository: UndoRepository) : BaseSoundEditViewModel() {

    override fun save(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(sounds, if (!multiple) name else null, volume, categoryId)
        undoRepository.pushState()
    }
}
