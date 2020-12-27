package us.huseli.soundboard.viewmodels

import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundEditViewModel(private val soundId: Int) : BaseSoundEditViewModel() {
    private val sound = repository.getLive(soundId)

    override val name = sound.map { it.name }
    override fun setName(value: String) {
        sound.value?.name = value
    }

    override val volume = sound.map { it.volume }
    override fun setVolume(value: Int) {
        sound.value?.volume = value
    }

    override fun setCategoryId(value: Int) {
        sound.value?.categoryId = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        sound.value?.let { sound ->
            // Check if category has changed, and if so, place sound last in new category
            repository.get(sound.id)?.let { originalSound ->
                sound.categoryId?.let { categoryId ->
                    if (originalSound.categoryId != categoryId) sound.order = repository.getMaxOrder(categoryId) + 1
                }
            }
            repository.update(sound)
        }
    }

    fun delete() = viewModelScope.launch(Dispatchers.IO) { repository.delete(soundId) }
}