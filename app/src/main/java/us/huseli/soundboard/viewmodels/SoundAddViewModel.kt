package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import javax.inject.Inject

@HiltViewModel
class SoundAddViewModel @Inject constructor(private val repository: SoundRepository) :
    BaseSoundEditViewModel() {
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
            onDuplicateStrategyChange()
        }

    private fun onDuplicateStrategyChange() {
        if (duplicateStrategy == DuplicateStrategy.SKIP) {
            removeSounds { sound -> sound.checksum in _duplicates.map { it.checksum } }
            if (sounds.size == 1) setName(sounds.first().name)
        }
    }

    fun setup(sounds: List<Sound>, allSounds: List<Sound>, multipleSoundsString: String) {
        super.setup(sounds, multipleSoundsString)
        _duplicates = allSounds.filter { sound ->
            sound.checksum != null && sound.checksum in sounds.mapNotNull { it.checksum }
        }
        onDuplicateStrategyChange()
    }

    override fun save(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        sounds.forEach { sound ->
            if (!multiple) sound.name = name
            newCategoryId?.let { sound.categoryId = it }
            sound.volume = volume
            when (duplicateStrategy) {
                DuplicateStrategy.UPDATE -> {
                    _duplicates.find { it.checksum == sound.checksum }?.let { duplicate ->
                        /**
                         * Duplicate is found and we want to update it. We have to do this by
                         * deleting the duplicate and putting the new sound in its place, though,
                         * because it's the new Uri that has been granted access permissions, and
                         * Sound.uri is read-only.
                         */
                        sound.order = duplicate.order
                        repository.delete(duplicate)
                    }
                    repository.insert(Sound.createFromTemporary(sound, context))
                }
                DuplicateStrategy.SKIP -> if (_duplicates.find { it.checksum == sound.checksum } == null)
                    repository.insert(Sound.createFromTemporary(sound, context))
                DuplicateStrategy.ADD -> repository.insert(Sound.createFromTemporary(sound, context))
            }
        }
    }


    enum class DuplicateStrategy { ADD, SKIP, UPDATE }
}