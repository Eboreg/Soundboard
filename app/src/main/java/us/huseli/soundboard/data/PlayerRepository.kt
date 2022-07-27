package us.huseli.soundboard.data

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.helpers.Functions
import us.huseli.soundboard.helpers.SettingsManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set

@Singleton
class PlayerRepository @Inject constructor(
    private val soundDao: SoundDao,
    private val settingsManager: SettingsManager
) : SoundPlayer.DurationListener, SettingsManager.Listener {

    private var bufferSize = Constants.DEFAULT_BUFFER_SIZE
    private var playersRaw = mutableMapOf<Int, SoundPlayer>()
    private val playersMutex = Mutex()
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    init {
        settingsManager.registerListener(this)
        // Check initial value
        scope.launch { onBufferSizeChange(settingsManager.getBufferSize()) }
    }

    private val playersMutableLiveData = MutableLiveData<Map<Int, SoundPlayer>>()

    val players = MediatorLiveData<Map<Int, SoundPlayer>>().apply {
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

    private fun updatePlayers(sounds: List<Sound>, players: Map<Int, SoundPlayer>) {
        /** Checks for relevant changes and update. Currently only volume. */
        Functions.warnIfOnMainThread("updatePlayers")
        sounds.forEach { sound ->
            sound.id?.let { soundId ->
                players[soundId]?.let { player ->
                    if (player.volume != sound.volume) {
                        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                            "updatePlayers: change volume (sound=$sound, sound volume=${sound.volume}, player=$player, player volume=${player.volume}")
                        player.setVolume(sound.volume)
                    }
                }
            }
        }
    }

    private fun removePlayers(sounds: List<Sound>, players: MutableMap<Int, SoundPlayer>) {
        Functions.warnIfOnMainThread("removePlayers")
        players.filterNot { player -> sounds.map { it.id }.contains(player.key) }.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "removePlayers: remove <sound=${it.key}, player=${it.value}>")
            it.value.release()
            players.remove(it.key)
        }
    }

    private fun addPlayers(sounds: List<Sound>, players: MutableMap<Int, SoundPlayer>) {
        Functions.warnIfOnMainThread("addPlayers")
        sounds.filterNot { players.contains(it.id) }.forEach { sound ->
            sound.id?.let { soundId -> players[soundId] = SoundPlayer(sound, bufferSize, this) }
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

    override fun onSettingChanged(key: String, value: Any?) {
        if (key == "bufferSize") scope.launch { onBufferSizeChange(value as Int) }
    }

    override fun onSoundPlayerDurationChange(sound: Sound, duration: Long) = scope.launch {
        sound.id?.let { soundId -> soundDao.updateDuration(soundId, duration) }
    }


    companion object {
        const val LOG_TAG = "PlayerRepository"
    }
}