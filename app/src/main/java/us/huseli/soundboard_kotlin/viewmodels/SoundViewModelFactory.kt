package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import us.huseli.soundboard_kotlin.data.Sound

class SoundViewModelFactory(private val sound: Sound) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Sound::class.java).newInstance(sound)
    }
}