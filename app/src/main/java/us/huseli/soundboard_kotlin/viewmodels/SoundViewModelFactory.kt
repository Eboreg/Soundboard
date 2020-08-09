package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import us.huseli.soundboard_kotlin.data.Sound

class SoundViewModelFactory(private val sound: Sound) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SoundViewModel(sound) as T
    }
}

