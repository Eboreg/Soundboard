package us.huseli.soundboard.viewmodels

import androidx.lifecycle.ViewModel
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Sound

abstract class BaseSoundEditViewModel : ViewModel() {
    /** Implemented by SoundAddViewModel and SoundEditViewModel */
    protected val sounds = mutableListOf<Sound>()

    var categoryId: Int? = null
    var categoryIndex: Int? = null
    var name = ""
    var volume = Constants.DEFAULT_VOLUME

    open val multiple: Boolean
        get() = soundCount > 1

    open val soundCount: Int
        get() = sounds.size

    open fun setup(sounds: List<Sound>, multipleSoundsString: String) {
        this.sounds.clear()
        this.sounds.addAll(sounds)
        if (!multiple) {
            name = sounds.first().name
            volume = sounds.first().volume
        } else {
            name = multipleSoundsString
            volume = if (sounds.groupBy { it.volume }.size == 1) sounds.first().volume else Constants.DEFAULT_VOLUME
        }
    }
}