package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

class SoundEditViewModel(private val sound: Sound) : ViewModel() {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    var name = sound.name
    var volume = sound.volume
    var categoryId: Int? = sound.categoryId
    var categoryIndex: Int? = null

    fun save() = viewModelScope.launch(Dispatchers.IO) {
        sound.name = name
        sound.volume = volume
        sound.categoryId = categoryId

        when (sound.id) {
            null -> repository.insert(sound)
            else -> repository.update(sound)
        }
    }
}