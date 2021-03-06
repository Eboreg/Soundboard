package us.huseli.soundboard.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
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
class PlayerRepository @Inject constructor(@ApplicationContext private val context: Context, soundDao: SoundDao) {
    private val _players = mutableListOf<SoundPlayer>()

    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "bufferSize") {
                prefs?.getInt(key, Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))?.let {
                    scope.launch { onBufferSizeChange(it) }
                }
            }
        }

    val players: LiveData<List<SoundPlayer>> = soundDao.listLiveWithCategory().map { soundsWithCategory ->
        _players.removeAll(playersToRemove(soundsWithCategory))
        _players.addAll(playersToAdd(soundsWithCategory))
        _players
    }

    private fun playersToAdd(soundsWithCategory: List<SoundWithCategory>): List<SoundPlayer> {
        /** Creates & returns SoundPlayers for those sounds that don't already have any */
        val sounds = soundsWithCategory.map { it.sound }
        return sounds.filterNot { _players.map { player -> player.sound }.contains(it) }.map { sound ->
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "addNew: add $sound")
            SoundPlayer(sound, bufferSize)
        }
    }

    private fun playersToRemove(soundsWithCategory: List<SoundWithCategory>): List<SoundPlayer> {
        /** Releases & returns SoundPlayers for those sounds NOT in `soundsWithCategory` */
        val sounds = soundsWithCategory.map { it.sound }
        return _players.filterNot { sounds.contains(it.sound) }.onEach { player ->
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "removeOld: remove ${player.sound}")
            player.release()
        }
    }

    init {
        scope.launch {
            PreferenceManager.getDefaultSharedPreferences(context).apply {
                registerOnSharedPreferenceChangeListener(preferenceListener)
                // Check initial value
                onBufferSizeChange(getInt("bufferSize",
                    Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)))
            }
        }
    }

    private fun onBufferSizeChange(seekbarValue: Int) {
        val newValue = Functions.seekbarValueToBufferSize(seekbarValue)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onBufferSizeChange: newValue=$newValue, bufferSize=$bufferSize")
        if (newValue != bufferSize) {
            bufferSize = newValue
            _players.forEach { player -> player.setBufferSize(newValue) }
        }
    }


    companion object {
        const val LOG_TAG = "PlayerRepository"
    }
}