package us.huseli.soundboard.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.data.PlayerRepository
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase

@Keep
class PlayerViewModel(soundId: Int) : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application)
    private val soundRepository = SoundRepository(database.soundDao())
    private val playerRepository = PlayerRepository()

    val player = soundRepository.getLive(soundId).switchMap {
        playerRepository.get(it)
    }

    override fun onCleared() {
        player.value?.release()
    }
}