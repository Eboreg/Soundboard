package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import us.huseli.soundboard.data.Sound

abstract class BaseSoundEditViewModel : ViewModel() {
    /** Implemented by SoundAddViewModel and SoundEditViewModel */
    private val _name = MutableLiveData("")
    private val _sounds = mutableListOf<Sound>()
    private val _volume = MutableLiveData(100)
    private var _multiple = false

    internal var categoryIndex: Int? = null

    internal val multiple: Boolean
        get() = _multiple

    val name: LiveData<String>
        get() = _name

    val sounds: List<Sound>
        get() = _sounds

    val volume: LiveData<Int>
        get() = _volume

    open fun setName(value: String) {
        _name.value = value
        if (!multiple) sounds.forEach { it.name = value }
    }

    open fun setup(sounds: List<Sound>, multipleSoundsString: String) {
        _sounds.clear()
        _sounds.addAll(sounds)
        if (_sounds.size == 1) {
            _multiple = false
            setName(_sounds.first().name)
            setVolume(_sounds.first().volume)
        } else {
            _multiple = true
            setName(multipleSoundsString)
            setVolume(100)
        }
    }

    open fun setVolume(value: Int) {
        _volume.value = value
        sounds.forEach { it.volume = value }
    }

    internal fun removeSounds(predicate: (Sound) -> Boolean): Boolean = _sounds.removeAll(predicate)

    abstract fun setCategoryId(value: Int): Any?
    abstract fun save(context: Context): Any?

}