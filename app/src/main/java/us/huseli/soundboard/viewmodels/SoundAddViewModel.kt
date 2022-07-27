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
class SoundAddViewModel @Inject constructor(
    private val repository: SoundRepository,
    private val undoRepository: UndoRepository
) : BaseSoundEditViewModel() {

    private var _duplicates = emptyList<Sound>()
    private val newSounds: List<Sound>
        get() = sounds.filter { sound -> sound.checksum !in duplicates.map { it.checksum } }
    private var order = -1

    val duplicateName: String
        get() = if (_duplicates.size == 1) _duplicates[0].name else ""
    val duplicates: List<Sound>
        get() = _duplicates
    val duplicateCount: Int
        get() = _duplicates.size
    val hasDuplicates: Boolean
        get() = _duplicates.isNotEmpty()

    override val soundCount: Int
        get() = if (duplicateStrategy == DuplicateStrategy.SKIP) newSounds.size else super.soundCount

    var duplicateStrategy = DuplicateStrategy.ADD

    fun setup(newSounds: List<Sound>, allSounds: List<Sound>, multipleSoundsString: CharSequence) {
        super.setup(newSounds, multipleSoundsString)
        order = -1
        _duplicates = allSounds.filter { sound -> sound.checksum in newSounds.map { it.checksum } }
    }

    /**
     * We could create and insert all the sounds in one batch, but we want to be able to display error messages for
     * individual sounds where Sound.createFromTemporary() throws an exception.
     */
    fun soundsToInsert() = (if (duplicateStrategy == DuplicateStrategy.ADD) sounds else newSounds).iterator()

    fun insertSound(tempSound: Sound, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryId?.let {
                if (order == -1) order = repository.getMaxOrder(it).plus(1)
                else order++
            }
            val sound = Sound.createFromTemporary(
                tempSound,
                if (!multiple) name.value.toString() else null,
                volume.value,
                categoryId,
                order,
                context
            )
            repository.insert(sound)
        }
    }

    fun updateExisting() = viewModelScope.launch(Dispatchers.IO) {
        if (duplicateStrategy == DuplicateStrategy.UPDATE) {
            repository.update(
                duplicates,
                if (!multiple) name.value.toString() else null,
                volume.value,
                categoryId
            )
        }
    }

    fun pushUndoState() = viewModelScope.launch(Dispatchers.IO) {
        undoRepository.pushState()
    }


    enum class DuplicateStrategy { ADD, SKIP, UPDATE }
}