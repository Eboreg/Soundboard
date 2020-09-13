package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.Sound

class SoundAddViewModel : BaseSoundEditViewModel() {
    private lateinit var _sound: Sound
    private val _name = MutableLiveData("")
    private val _volume = MutableLiveData(100)

    override val name: LiveData<String>
        get() = _name
    override val volume: LiveData<Int>
        get() = _volume

    fun setup(sound: Sound) {
        _sound = sound
        setName(sound.name)
        setVolume(sound.volume)
    }

    override fun setName(value: String) {
        _sound.name = value
        _name.value = value
    }

    override fun setVolume(value: Int) {
        _sound.volume = value
        _volume.value = value
    }

    override fun setCategoryId(value: Int) {
        _sound.categoryId = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(_sound)
    }
}