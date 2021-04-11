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
    val duplicateName: String
        get() = if (_duplicates.size == 1) _duplicates[0].name else ""
    val duplicates: List<Sound>
        get() = _duplicates
    val duplicateCount: Int
        get() = _duplicates.size
    val hasDuplicates: Boolean
        get() = _duplicates.isNotEmpty()

    var duplicateStrategy = DuplicateStrategy.ADD
        set(value) {
            field = value
            if (value == DuplicateStrategy.SKIP) {
                removeSounds { sound -> sound.checksum in _duplicates.map { it.checksum } }
                if (sounds.size == 1) setName(sounds.first().name)
            }
        }

    fun setup(newSounds: List<Sound>, allSounds: List<Sound>, multipleSoundsString: String) {
        super.setup(newSounds, multipleSoundsString)
        _duplicates = allSounds.filter { sound ->
            sound.checksum != null && sound.checksum in newSounds.mapNotNull { it.checksum }
        }
    }

    override fun setSoundAttrsBeforeSave(sound: Sound): Sound {
        return super.setSoundAttrsBeforeSave(sound).apply {
            newCategoryId?.let { categoryId = it }
        }
    }

    override fun doSave(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        sounds.forEach { sound ->
            when (duplicateStrategy) {
                DuplicateStrategy.UPDATE -> {
                    _duplicates.find { it.checksum == sound.checksum }?.let { duplicate ->
                        repository.update(setSoundAttrsBeforeSave(duplicate))
                    } ?: repository.insert(Sound.createFromTemporary(sound, context))
                }
                DuplicateStrategy.SKIP -> if (_duplicates.find { it.checksum == sound.checksum } == null)
                    repository.insert(Sound.createFromTemporary(sound, context))
                DuplicateStrategy.ADD -> repository.insert(Sound.createFromTemporary(sound, context))
            }
        }
        undoRepository.pushSoundState()
    }


    enum class DuplicateStrategy { ADD, SKIP, UPDATE }
}