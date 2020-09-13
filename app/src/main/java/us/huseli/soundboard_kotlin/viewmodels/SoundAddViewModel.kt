package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.Sound

class SoundAddViewModel(private val sound: Sound) : BaseSoundEditViewModel() {
    override val name = liveData { emit(sound.name) }
    override val volume = liveData { emit(sound.volume) }

    override fun setName(value: String) {
        sound.name = value
    }

    override fun setVolume(value: Int) {
        sound.volume = value
    }

    override fun setCategoryId(value: Int) {
        sound.categoryId = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(sound)
    }
}