package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication

class SoundEditViewModel(soundId: Int) : BaseSoundEditViewModel() {
    private val sound = repository.get(soundId)

    override val name = sound.map { it.name }
    override fun setName(value: String) {
        sound.value?.name = value
    }

    override val volume = sound.map { it.volume }
    override fun setVolume(value: Int) {
        sound.value?.let {
            if (it.volume != value) {
                val player = GlobalApplication.application.getPlayer(it)
                player.setVolume(value)
                it.volume = value
            }
        }
    }

    override val categoryId = sound.map { it.categoryId }
    override fun setCategoryId(value: Int) {
        sound.value?.categoryId = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        sound.value?.let { sound -> repository.update(sound) }
    }
}