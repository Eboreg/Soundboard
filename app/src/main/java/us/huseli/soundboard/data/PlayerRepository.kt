package us.huseli.soundboard.data

import androidx.lifecycle.liveData
import us.huseli.soundboard.SoundPlayer

class PlayerRepository {
    private val _players = HashMap<Sound, SoundPlayer>()

    private fun addOrUpdate(sound: Sound): SoundPlayer {
        return _players[sound] ?: SoundPlayer(sound).also { _players[sound] = it }
    }

    private fun addOrUpdate(sounds: List<Sound>): List<SoundPlayer> {
        val players = mutableListOf<SoundPlayer>()
        sounds.forEach { addOrUpdate(it).let { player -> players.add(player) } }
        return players
    }

    fun get(sound: Sound?) = liveData {
        emit(sound?.let { addOrUpdate(sound) })
    }

}