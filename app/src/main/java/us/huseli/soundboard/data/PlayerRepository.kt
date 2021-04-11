package us.huseli.soundboard.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.helpers.Functions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set

@Singleton
class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context, private val soundDao: SoundDao) : SoundPlayer.DurationListener {

    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE
    private var playersRaw = mutableMapOf<Sound, SoundPlayer>()
    private val playersMutex = Mutex()
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "bufferSize") {
                prefs?.getInt(key, Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))?.let {
                    scope.launch { onBufferSizeChange(it) }
                }
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

    private val playersMutableLiveData = MutableLiveData<Map<Sound, SoundPlayer>>()

    val players = MediatorLiveData<Map<Sound, SoundPlayer>>().apply {
        addSource(soundDao.listLive()) { sounds ->
            scope.launch {
                playersMutex.withLock {
                    removePlayers(sounds, playersRaw)
                    updatePlayers(sounds, playersRaw)
                    addPlayers(sounds, playersRaw)
                }
                playersMutableLiveData.postValue(playersRaw)
            }
        }
        addSource(playersMutableLiveData) { value = it }
    }

    private fun updatePlayers(sounds: List<Sound>, players: Map<Sound, SoundPlayer>) {
        /** Checks for relevant changes and update. Currently only volume. */
        Functions.warnIfOnMainThread("updatePlayers")
        sounds.forEach { sound ->
            players[sound]?.let { player ->
                if (player.volume != sound.volume) {
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                        "updatePlayers: change volume (sound=$sound, sound volume=${sound.volume}, player=$player, player volume=${player.volume}")
                    player.setVolume(sound.volume)
                }
            }
        }
    }

    private fun removePlayers(sounds: List<Sound>, players: MutableMap<Sound, SoundPlayer>) {
        Functions.warnIfOnMainThread("removePlayers")
        players.filterNot { sounds.contains(it.key) }.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "removePlayers: remove <sound=${it.key}, player=${it.value}>")
            it.value.release()
            players.remove(it.key)
        }
    }

    private fun addPlayers(sounds: List<Sound>, players: MutableMap<Sound, SoundPlayer>) {
        Functions.warnIfOnMainThread("addPlayers")
        sounds.filterNot { players.contains(it) }.forEach { sound ->
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "addPlayers: add $sound")
            players[sound] = SoundPlayer(sound, bufferSize, this)
        }
    }

    private suspend fun onBufferSizeChange(seekbarValue: Int) {
        val newValue = Functions.seekbarValueToBufferSize(seekbarValue)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onBufferSizeChange: newValue=$newValue, bufferSize=$bufferSize")
        if (newValue != bufferSize) {
            bufferSize = newValue
            playersMutex.withLock {
                playersRaw.forEach { it.value.setBufferSize(newValue) }
            }
        }
    }

    override fun onSoundPlayerDurationChange(sound: Sound, duration: Long) {
        sound.id?.let { soundId -> soundDao.updateDuration(soundId, duration) }
    }


    companion object {
        const val LOG_TAG = "PlayerRepository"
    }
}