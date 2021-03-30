package us.huseli.soundboard.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Sound

abstract class BaseSoundEditViewModel : ViewModel() {
    /** Implemented by SoundAddViewModel and SoundEditViewModel */
    private var _name = ""
    private val _sounds = mutableListOf<Sound>()
    private var _volume = Constants.DEFAULT_VOLUME

    protected var newCategoryId: Int? = null

    var categoryIndex: Int? = null

    val multiple: Boolean
        get() = _sounds.size > 1

    val name: String
        get() = _name

    val sounds: List<Sound>
        get() = _sounds

    val volume: Int
        get() = _volume

    fun save(context: Context) {
        sounds.forEach { setSoundAttrsBeforeSave(it) }
        doSave(context)
    }

    fun setCategoryId(value: Int) {
        newCategoryId = value
    }

    fun setName(value: String) {
        _name = value
    }

    open fun setSoundAttrsBeforeSave(sound: Sound): Sound {
        if (!multiple) sound.name = name
        sound.volume = volume
        return sound
    }

    open fun setup(sounds: List<Sound>, multipleSoundsString: String) {
        _sounds.clear()
        _sounds.addAll(sounds)
        if (!multiple) {
            _name = sounds.first().name
            _volume = sounds.first().volume
        } else {
            _name = multipleSoundsString
            _volume = if (sounds.groupBy { it.volume }.size == 1) sounds.first().volume else Constants.DEFAULT_VOLUME
        }
    }

    fun setVolume(value: Int) {
        _volume = value
    }

    protected fun removeSounds(predicate: (Sound) -> Boolean): Boolean = _sounds.removeAll(predicate)

    protected abstract fun doSave(context: Context): Any?
}