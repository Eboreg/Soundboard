package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Sound

class SoundAddViewModel : BaseSoundEditViewModel() {
    private var _duplicates = emptyList<Sound>()
    private val _name = MutableLiveData("")
    private val _sounds = mutableListOf<Sound>()
    private val _volume = MutableLiveData(100)

    override val name: LiveData<String>
        get() = _name
    override val volume: LiveData<Int>
        get() = _volume

    val duplicateName: String
        get() = if (_duplicates.size == 1) _duplicates[0].name else ""
    val duplicates: List<Sound>
        get() = _duplicates
    val duplicateCount: Int
        get() = _duplicates.size
    val hasDuplicates: Boolean
        get() = _duplicates.isNotEmpty()
    val sounds: List<Sound>
        get() = _sounds

    var duplicateStrategy = DuplicateStrategy.ADD
        set(value) {
            field = value
            onDuplicateStrategyChange()
        }

    private fun onDuplicateStrategyChange() {
        if (duplicateStrategy == DuplicateStrategy.SKIP) {
            _sounds.removeAll { sound -> sound.checksum in _duplicates.map { it.checksum } }
            if (_sounds.size == 1) setName(_sounds[0].name)
        }
    }

    private fun setup(sounds: List<Sound>, allSounds: List<Sound>, name: String, volume: Int) {
        _sounds.clear()
        _sounds.addAll(sounds)
        _duplicates = allSounds.filter { sound ->
            sound.checksum != null && sound.checksum in sounds.mapNotNull { it.checksum }
        }
        onDuplicateStrategyChange()
        setName(name)
        setVolume(volume)
    }

    fun setup(sounds: List<Sound>, allSounds: List<Sound>, multipleSoundsString: String) {
        if (sounds.size == 1) setup(sounds, allSounds, sounds[0].name, sounds[0].volume)
        else setup(sounds, allSounds, multipleSoundsString, 100)
    }

    fun setup(sound: Sound, allSounds: List<Sound>) =
            setup(listOf(sound), allSounds, sound.name, sound.volume)

    override fun setName(value: String) {
        _name.value = value
    }

    override fun setVolume(value: Int) {
        _volume.value = value
        _sounds.forEach { it.volume = value }
    }

    override fun setCategoryId(value: Int) = _sounds.forEach { it.categoryId = value }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        //GlobalApplication.pushUndoState(_sounds, null)
        _sounds.forEach { sound ->
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
                    repository.insert(Sound.createFromTemporary(sound))
                }
                DuplicateStrategy.SKIP -> if (_duplicates.find { it.checksum == sound.checksum } == null) repository.insert(Sound.createFromTemporary(sound))
                DuplicateStrategy.ADD -> repository.insert(Sound.createFromTemporary(sound))
            }
        }
    }


    enum class DuplicateStrategy { ADD, SKIP, UPDATE }
}