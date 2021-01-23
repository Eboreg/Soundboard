package us.huseli.soundboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.liveData
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import us.huseli.soundboard.Constants
import us.huseli.soundboard.SoundPlayer
import us.huseli.soundboard.helpers.Functions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(@ApplicationContext context: Context) {
    private val _players = HashMap<Sound, SoundPlayer>()
    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "bufferSize") {
            prefs?.getInt(key, Constants.DEFAULT_BUFFER_SIZE)?.let {
                val newValue = Functions.seekbarValueToBufferSize(it)
                if (newValue != bufferSize) {
                    bufferSize = newValue
                    _players.values.forEach { player -> player.setBufferSize(newValue) }
                }
            }
        }
    }

    init {
        PreferenceManager.getDefaultSharedPreferences(context).apply {
            registerOnSharedPreferenceChangeListener(preferenceListener)
            bufferSize = Functions.seekbarValueToBufferSize(getInt("bufferSize", Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)))
        }
    }

    private fun addOrUpdate(sound: Sound): SoundPlayer? {
        return sound.uri.path?.let { path ->
            _players[sound] ?: SoundPlayer(sound, path, bufferSize).also { _players[sound] = it }
        }
    }

    fun get(sound: Sound?) = liveData(Dispatchers.Default) {
        emit(sound?.let { addOrUpdate(sound) })
    }
}