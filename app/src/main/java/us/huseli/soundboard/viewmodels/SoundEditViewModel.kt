package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.SoundRepository
import javax.inject.Inject

@HiltViewModel
class SoundEditViewModel @Inject constructor(private val repository: SoundRepository) :
    BaseSoundEditViewModel() {
    private var _newCategoryId: Int? = null

    override fun save(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        // Check if category has changed, and if so, place sounds last in new category
        _newCategoryId?.let { categoryId ->
            var order = repository.getMaxOrder(categoryId)
            sounds.forEach { sound ->
                if (sound.categoryId != categoryId) {
                    sound.categoryId = categoryId
                    sound.order = ++order
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

