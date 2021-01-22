package us.huseli.soundboard.data

import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import us.huseli.soundboard.SoundPlayer
import javax.inject.Inject

class PlayerRepository @Inject constructor() {
    private val _players = HashMap<Sound, SoundPlayer>()

    private fun addOrUpdate(sound: Sound): SoundPlayer? {
        return sound.uri.path?.let { path ->
            _players[sound] ?: SoundPlayer(sound, path).also { _players[sound] = it }
        }
    }

    fun get(sound: Sound?) = liveData(Dispatchers.Default) {
        emit(sound?.let { addOrUpdate(sound) })
    }
}