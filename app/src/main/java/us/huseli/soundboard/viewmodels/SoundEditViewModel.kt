package us.huseli.soundboard.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundEditViewModel : BaseSoundEditViewModel() {
    private var _newCategoryId: Int? = null

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        if (!multiple) {
            // Check if category has changed, and if so, place sound last in new category
            _newCategoryId?.let { newCategoryId ->
                if (newCategoryId != sounds.first().categoryId)
                    sounds.first().order = repository.getMaxOrder(newCategoryId) + 1
            }
        } else {
            _newCategoryId?.let { categoryId ->
                var order = repository.getMaxOrder(categoryId)
                sounds.forEach { sound ->
                    if (sound.categoryId != categoryId) {
                        sound.categoryId = categoryId
                        sound.order = ++order
                    }
                }
            }
        }
        volume.value?.let { volume -> sounds.forEach { it.volume = volume } }
        repository.update(sounds)
    }

    override fun setCategoryId(value: Int) {
        _newCategoryId = value
    }
}

