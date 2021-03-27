package us.huseli.soundboard.data

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
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
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context, private val soundDao: SoundDao) : SoundPlayer.DurationListener {

    private val _players = mutableMapOf<Sound, SoundPlayer>()
    private var effect: AudioEffect? = null
    private var effectSendLevel = 0f
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "bufferSize") {
                prefs?.getInt(key, Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))?.let {
                    scope.launch { onBufferSizeChange(it) }
                }
            }
        }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE

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

    val players: LiveData<Map<Sound, SoundPlayer>> = soundDao.listLiveWithCategory().map { soundsWithCategory ->
        val sounds = soundsWithCategory.map { it.sound }
        removePlayers(sounds)
        addPlayers(sounds)
        updatePlayers(sounds)
        _players
    }

    fun setEffect(effect: AudioEffect, sendLevel: Float) {
        this.effect = effect
        effectSendLevel = sendLevel
        _players.values.forEach { it.setEffect(effect, sendLevel) }
    }

    fun unsetEffect() {
        effect = null
        effectSendLevel = 0f
        _players.values.forEach { it.unsetEffect() }
    }

    private fun updatePlayers(sounds: List<Sound>) {
        /** Checks for relevant changes and update. Currently only volume. */
        sounds.forEach { sound ->
            _players[sound]?.let { player ->
                if (player.volume != sound.volume) player.setVolume(sound.volume)
            }
        }
    }

    private fun removePlayers(sounds: List<Sound>) {
        _players.filterNot { sounds.contains(it.key) }.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "removePlayers: remove ${it.key}")
            it.value.release()
            _players.remove(it.key)
        }
    }

    private fun addPlayers(sounds: List<Sound>) {
        sounds.filterNot { _players.contains(it) }.forEach { sound ->
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "addPlayers: add $sound")
            _players[sound] = SoundPlayer(sound, bufferSize, effect, effectSendLevel, this)
        }
    }

    private fun onBufferSizeChange(seekbarValue: Int) {
        val newValue = Functions.seekbarValueToBufferSize(seekbarValue)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onBufferSizeChange: newValue=$newValue, bufferSize=$bufferSize")
        if (newValue != bufferSize) {
            bufferSize = newValue
            _players.forEach { it.value.setBufferSize(newValue) }
        }
    }

    override fun onSoundPlayerDurationChange(sound: Sound, duration: Int) {
        sound.id?.let { soundId -> soundDao.updateDuration(soundId, duration) }
    }


    companion object {
        const val LOG_TAG = "PlayerRepository"
    }
}