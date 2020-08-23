package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.data.Sound

class SoundEditViewModel(private val sound: Sound?) : ViewModel() {
    var name = sound?.name ?: ""
    var volume = sound?.volume ?: 100
    var categoryIndex: Int? = null
}