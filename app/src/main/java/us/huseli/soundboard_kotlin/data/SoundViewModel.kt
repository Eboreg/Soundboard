package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundRepository(application)

    val sounds: LiveData<List<Sound>> by lazy { repository.sounds }

    fun insertSound(sound: Sound) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(sound)
    }

    fun updateSoundName(soundId: Int, name: String) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateName(soundId, name)
    }

    fun deleteSound(soundId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(soundId)
    }
}