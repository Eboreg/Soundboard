package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import us.huseli.soundboard_kotlin.data.EmptySound

class EmptySoundViewModelFactory(private val sound: EmptySound, private val adapterPosition: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        // modelClass = EmptySoundViewModel
        // position comes from SoundAdapter.bind()
        return modelClass.getConstructor(EmptySound::class.java, Int::class.java).newInstance(sound, adapterPosition)
    }
}