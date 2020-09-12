package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

class SoundListViewModel : ViewModel() {
    //constructor() : this(null)

    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    //val soundViewModels = sounds.map { it.map { sound -> SoundViewModel(sound) } }

    //fun getSoundsByCategory(categoryId: Int) = repository.sounds.map { list -> list.filter { it.categoryId == categoryId } }

/*
    fun getSoundViewModels(categoryId: Int)
            = repository.sounds.map { list -> list.filter { it.categoryId == categoryId } }.map { it.map { sound -> SoundViewModel(sound) } }
*/

    fun delete(soundId: Int) = viewModelScope.launch(Dispatchers.IO) { repository.delete(soundId) }

    fun update(sound: Sound) = viewModelScope.launch(Dispatchers.IO) { repository.update(sound) }

    fun updateOrder(sounds: List<Sound>) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateOrder(sounds)
    }
}