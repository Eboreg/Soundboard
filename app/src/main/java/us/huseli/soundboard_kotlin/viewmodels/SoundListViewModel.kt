package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundDatabase
import us.huseli.soundboard_kotlin.data.SoundRepository
import java.util.*

class SoundListViewModel : ViewModel {
    private val repository = SoundRepository(SoundDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())
    private val sounds: LiveData<List<Sound>>
    val soundViewModels: LiveData<List<SoundViewModel>>

    constructor() : super() {
        sounds = repository.sounds
        soundViewModels = Transformations.switchMap(sounds) {
            liveData { emit(it.map { sound -> SoundViewModel(sound) }) }
        }
    }

    constructor(categoryId: Int) : super() {
        sounds = repository.soundsByCategory(categoryId)
        soundViewModels = Transformations.switchMap(sounds) {
            liveData { emit(it.map { sound -> SoundViewModel(sound) }) }
        }
    }

    fun getSoundViewModel(sound: Sound): SoundViewModel = SoundViewModel(sound)

    fun getSoundViewModel(soundId: Int): SoundViewModel? =
            sounds.value?.find { it.id == soundId }?.let { sound -> getSoundViewModel(sound) }

    // We get this from SoundListFragment, and the positions refer to those in our `sounds`
    fun updateSoundOrder(fromPosition: Int, toPosition: Int) = viewModelScope.launch(Dispatchers.IO) {
        sounds.value?.let {
            if (fromPosition < toPosition)
                for (i in fromPosition until toPosition) Collections.swap(it, i, i + 1)
            else
                for (i in fromPosition downTo toPosition + 1) Collections.swap(it, i, i - 1)
            it.forEachIndexed { idx, s ->
                if (s.order != idx) {
                    s.order = idx
                    repository.update(s)
                }
            }
        }
    }

    fun deleteSound(soundId: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(soundId)
    }
}