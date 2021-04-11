package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject

@HiltViewModel
class SoundEditViewModel @Inject constructor(
    private val repository: SoundRepository, private val undoRepository: UndoRepository) : BaseSoundEditViewModel() {

    override fun setup(sounds: List<Sound>, multipleSoundsString: String) {
        /**
         * TODO: This decoupling could maybe be made better by making most Sound parameters val's, and only update
         * them in soundDao?
         *
         * When editing sounds, we operate on copies of them, since there seems to be no reasonable way to avoid
         * UI diskrepancies otherwise.
         * When we, for example, update sound.volume in save(), and that sound is the same object that SoundAdapter
         * keeps a reference to, DiffUtil won't recognise that the sound's volume has changed the next time it's run,
         * because it will compare a newly fetched object to the one we already updated here!
         */
        super.setup(sounds.map { it.copy() }, multipleSoundsString)
    }

    override fun doSave(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        // Check if category has changed, and if so, place sounds last in new category
        newCategoryId?.let { categoryId ->
            var order = repository.getMaxOrder(categoryId)
            sounds.forEach { sound ->
                if (sound.categoryId != categoryId) {
                    sound.categoryId = categoryId
                    sound.order = ++order
                }
            }
        }
        repository.update(sounds)
        undoRepository.pushSoundState()
    }
}
