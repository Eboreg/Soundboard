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
    private val repository: SoundRepository, private val undoRepository: UndoRepository) : BaseSoundEditViewModel() {

    private var _duplicates = emptyList<Sound>()
    private val newSounds: List<Sound>
        get() = sounds.filter { sound -> sound.checksum != null && sound.checksum !in duplicates.mapNotNull { it.checksum } }

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

    fun setup(newSounds: List<Sound>, allSounds: List<Sound>, multipleSoundsString: String) {
        super.setup(newSounds, multipleSoundsString)
        _duplicates = allSounds.filter { sound ->
            sound.checksum != null && sound.checksum in newSounds.mapNotNull { it.checksum }
        }
    }

    override fun save(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val toUpdate = mutableListOf<Sound>()
        val toInsert = mutableListOf<Sound>()

        when (duplicateStrategy) {
            DuplicateStrategy.ADD -> toInsert.addAll(sounds)
            DuplicateStrategy.SKIP -> toInsert.addAll(newSounds)
            DuplicateStrategy.UPDATE -> {
                toInsert.addAll(newSounds)
                toUpdate.addAll(duplicates)
            }
        }

        repository.update(toUpdate, if (!multiple) name else null, volume, categoryId)
        repository.insert(Sound.createFromTemporary(toInsert,
            if (!multiple) name else null,
            volume,
            categoryId,
            context))

        undoRepository.pushState()
    }


    enum class DuplicateStrategy { ADD, SKIP, UPDATE }
}