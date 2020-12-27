package us.huseli.soundboard.viewmodels

import androidx.lifecycle.ViewModel
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase

class SoundViewModel(soundId: Int) : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application)
    private val soundRepository = SoundRepository(database.soundDao())

    val sound = soundRepository.getLive(soundId)
}