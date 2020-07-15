package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SoundRepository

    val sounds: LiveData<List<Sound>>
    val reorderEnabled: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }
    val zoomLevel: MutableLiveData<Int> by lazy { MutableLiveData(0) }

    init {
        val dao = SoundDatabase.getInstance(application, viewModelScope).soundDao()
        repository = SoundRepository(dao)
        sounds = repository.sounds
    }

    // We get this from SoundListFragment, and the positions refer to those in our `sounds`
    fun updateSoundOrder(fromPosition: Int, toPosition: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateOrder(fromPosition, toPosition)
    }

    fun deleteSound(soundId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(soundId)
    }
}