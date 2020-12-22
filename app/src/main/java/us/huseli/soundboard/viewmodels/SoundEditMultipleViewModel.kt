package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundEditMultipleViewModel : BaseSoundEditViewModel() {
    private val _soundIds = mutableListOf<Int>()
    private val _name = MutableLiveData("")
    private val _volume = MutableLiveData(100)
    private var _newCategoryId: Int? = null

    // Will really only be a placeholder string like 'multiple sounds selected'
    override val name: LiveData<String>
        get() = _name
    override val volume: LiveData<Int>
        get() = _volume

    override fun setName(value: String) {
        _name.value = value
    }

    override fun setVolume(value: Int) {
        _volume.value = value
    }

    override fun setCategoryId(value: Int) {
        _newCategoryId = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        val sounds = repository.list(_soundIds)
        _volume.value?.let { volume -> sounds.forEach { it.volume = volume } }
        _newCategoryId?.let { categoryId ->
            var order = repository.getMaxOrder(categoryId)
            sounds.forEach { sound ->
                if (sound.categoryId != categoryId) {
                    sound.categoryId = categoryId
                    sound.order = ++order
                }
            }
        }
        repository.update(sounds)
    }

    fun setup(soundIds: List<Int>, name: String) {
        _soundIds.clear()
        _soundIds.addAll(soundIds)
        setName(name)
        setVolume(100)
    }
}