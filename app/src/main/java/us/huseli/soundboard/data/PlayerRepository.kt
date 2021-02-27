package us.huseli.soundboard.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    private val _players = mutableListOf<SoundPlayer>()
    private val _playersLive = MutableLiveData<List<SoundPlayer>>(emptyList())

    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    val players: LiveData<List<SoundPlayer>>
        get() = _playersLive

    init {
        scope.launch {
            PreferenceManager.getDefaultSharedPreferences(context).apply {
                registerOnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "bufferSize") {
                        prefs?.getInt(key, Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))?.also {
                            onBufferSizeChange(it)
                        }
                    }
                }
                // Check initial value
                onBufferSizeChange(getInt("bufferSize",
                    Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)))
            }
        }
    }

    private fun onBufferSizeChange(seekbarValue: Int) {
        val newValue = Functions.seekbarValueToBufferSize(seekbarValue)
        if (newValue != bufferSize) {
            bufferSize = newValue
            _players.forEach { player -> player.setBufferSize(newValue) }
        }
    }

    fun set(sounds: List<Sound>) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: starting, sounds=$sounds, _players=$_players")

        // 1. Release SoundPlayers whose sounds are not in list
        _players.filter { !sounds.contains(it.sound) && it.state != SoundPlayer.State.RELEASED }.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: release ${it.sound}")
            it.release()
        }

        // 2. Add SoundPlayers for sounds that don't have any
        val newSounds = sounds.filterNot { _players.map { player -> player.sound }.contains(it) }
        if (newSounds.isNotEmpty()) {
            newSounds.forEach { sound ->
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: add $sound")
                _players.add(SoundPlayer(sound, bufferSize))
            }
            // 2.1. Replace players.value with new list
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: postValue $_players")
            _playersLive.postValue(_players)
        }

        // 3. Reinit SoundPlayers whose sounds are in list but were previously released
        _players.filter { sounds.contains(it.sound) && it.state == SoundPlayer.State.RELEASED }.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "set: reinit $it")
            it.reinit()
        }
    }


    companion object {
        const val LOG_TAG = "PlayerRepository"
    }
}