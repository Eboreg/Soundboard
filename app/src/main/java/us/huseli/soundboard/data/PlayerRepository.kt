package us.huseli.soundboard.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.liveData
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.helpers.Functions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(@ApplicationContext context: Context) {
    private val _players = HashMap<Sound, SoundPlayer>()
    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var previousSounds: List<Sound>? = null
    private var setJob: Job? = null
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
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
        scope.launch {
            PreferenceManager.getDefaultSharedPreferences(context).apply {
                registerOnSharedPreferenceChangeListener(preferenceListener)
                bufferSize = Functions.seekbarValueToBufferSize(
                    getInt(
                        "bufferSize", Functions.bufferSizeToSeekbarValue(
                            Constants.DEFAULT_BUFFER_SIZE
                        )
                    )
                )
            }
        }
    }

    private fun addIfNotExists(sound: Sound) {
        if (!_players.containsKey(sound)) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "addIfNotExists: add $sound")
            _players[sound] = SoundPlayer(sound, bufferSize)
        }
    }

    fun set(sounds: List<Sound>) {
        if (BuildConfig.DEBUG) {
            if (sounds.isNotEmpty()) Log.d(
                LOG_TAG,
                "set: sounds=${sounds.first()} to ${sounds.last()}"
            )
            else Log.d(LOG_TAG, "set: sounds is empty")
        }
        // Release & remove players whose sound is not in `sounds`
        _players.filterNot { sounds.contains(it.key) }.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: remove ${it.key}")
            it.value.release()
            _players.remove(it.key)
        }
        sounds.forEach { addIfNotExists(it) }

/*
        if (sounds != previousSounds) {
            setJob?.cancel()
            setJob = scope.launch {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: sounds=${sounds.first()} to ${sounds.last()}")
                // Release & remove players whose sound is not in `sounds`
                _players.filterNot { sounds.contains(it.key) }.forEach {
                    if (isActive) {
                        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: remove ${it.key}")
                        it.value.release()
                        _players.remove(it.key)
                    }
                }
                sounds.forEach {
                    if (isActive) addIfNotExists(it)
                }
                previousSounds = sounds
            }
        }
*/
    }

    fun get(sound: Sound?) = liveData(Dispatchers.Default) {
        emit(sound?.let {
            addIfNotExists(sound)
            _players[sound]
        })
    }


    companion object {
        const val LOG_TAG = "PlayerRepository"
    }
}