package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.Sound

class SoundAddMultipleViewModel : BaseSoundEditViewModel() {
    private val _sounds = mutableListOf<Sound>()
    private val _name = MutableLiveData("")
    private val _volume = MutableLiveData(100)

    // Will really only be a placeholder string like 'multiple sounds selected'
    override val name: LiveData<String>
        get() = _name
    override val volume: LiveData<Int>
        get() = _volume

    fun setup(sounds: List<Sound>, name: String) {
        _sounds.clear()
        _sounds.addAll(sounds)
        setName(name)
        setVolume(100)
    }

    override fun setName(value: String) {
        _name.value = value
    }

    override fun setVolume(value: Int) {
        _volume.value = value
        _sounds.forEach { it.volume = value }
    }

    override fun setCategoryId(value: Int?) = value?.let { _sounds.forEach { it.categoryId = value } }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        _sounds.forEach { repository.insert(it) }
    }
}