package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.Sound

class SoundEditMultipleViewModel : BaseSoundEditViewModel() {
    private val _sounds = mutableListOf<Sound>()
    private val _name = MutableLiveData("")
    private val _volume = MutableLiveData(100)
    private val _originalCategoryIds = mutableMapOf<Sound, Int?>()
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
        _sounds.forEach { it.volume = value }
    }

    override fun setCategoryId(value: Int) {
        _newCategoryId = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        _newCategoryId?.let { categoryId ->
            var order = repository.getMaxOrder(categoryId)
            _sounds.forEach { sound ->
                if (_originalCategoryIds[sound] != categoryId) {
                    sound.categoryId = categoryId
                    sound.order = ++order
                }
            }
        }
        repository.update(_sounds)
    }

    fun setup(sounds: List<Sound>, name: String) {
        _sounds.clear()
        _sounds.addAll(sounds)
        _originalCategoryIds.clear()
        sounds.forEach { _originalCategoryIds[it] = it.categoryId }
        setName(name)
        setVolume(100)
    }
}