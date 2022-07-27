package us.huseli.soundboard.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.Functions

abstract class BaseSoundEditViewModel : ViewModel() {
    /** Implemented by SoundAddViewModel and SoundEditViewModel */
    protected val sounds = mutableListOf<Sound>()
    private var _categoryId: Int? = null
    private var _categoryIndex: Int? = null
    private val _name = MutableLiveData<CharSequence>("")
    private val _volume = MutableLiveData(Constants.DEFAULT_VOLUME)

    val categoryId: Int?
        get() = _categoryId
    fun setCategoryId(value: Int) {
        _categoryId = value
    }

    val categoryIndex: Int?
        get() = _categoryIndex
    fun setCategoryIndex(value: Int) {
        _categoryIndex = value
    }

    val name: LiveData<CharSequence>
        get() = _name
    fun setName(value: CharSequence) {
        _name.value = value
    }

    val volume: LiveData<Int>
        get() = _volume
    fun setVolume(value: Int) {
        _volume.value = value
    }

    open val multiple: Boolean
        get() = soundCount > 1

    open val soundCount: Int
        get() = sounds.size

    open fun setup(sounds: List<Sound>, multipleSoundsString: CharSequence) {
        this.sounds.clear()
        this.sounds.addAll(sounds)
        if (!multiple) {
            _name.value = sounds.first().name
            _volume.value = sounds.first().volume
        } else {
            _name.value = Functions.umlautify(multipleSoundsString).toString()
            _volume.value = if (sounds.groupBy { it.volume }.size == 1) sounds.first().volume else Constants.DEFAULT_VOLUME
        }
    }
}