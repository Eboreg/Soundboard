package us.huseli.soundboard.data

import androidx.lifecycle.liveData
import us.huseli.soundboard.SoundPlayer

class PlayerRepository {
    private val _players = HashMap<Sound, SoundPlayer>()

    private fun addOrUpdate(sound: Sound): SoundPlayer {
        return _players[sound] ?: SoundPlayer(sound).also { _players[sound] = it }
    }

    fun get(sound: Sound?) = liveData {
        emit(sound?.let { addOrUpdate(sound) })
    }
}