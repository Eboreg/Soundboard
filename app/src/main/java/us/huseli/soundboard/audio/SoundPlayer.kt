package us.huseli.soundboard.audio

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Sound

class SoundPlayer(private val sound: Sound,
                  private var bufferSize: Int,
                  private var durationListener: DurationListener?) : AudioFile.Listener {
    private var _duration: Long = -1
        set(value) {
            if (value != field) {
                field = value
                if (value > -1) durationListener?.onSoundPlayerDurationChange(sound, value)
            }
        }
    private var _errorMessage = ""
    private var _state = State.INITIALIZING
        set(value) {
            stateListener?.onSoundPlayerStateChange(value, playbackPositionMs)
            field = value
        }
    private var _volume = sound.volume
    private var audioFile: AudioFile? = null
    private var job: Job? = null
    private var stateListener: StateListener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val tempAudioFiles = mutableListOf<AudioFile>()
    private val tempAudioFileMutex = Mutex()

    val playbackPositionMs: Long
        get() = audioFile?.playbackPositionMs ?: 0

    var repressMode = RepressMode.STOP
        set(value) {
            if (value != field && field == RepressMode.PAUSE && _state == State.PAUSED) {
                // Mode was PAUSE and is not anymore, meaning we "unpause" any paused sound
                audioFile?.also { if (it.isPaused) scope.launch { it.prepareAndPrime() } }
            }
            field = value
        }

    val duration: Long
        get() = _duration
    val errorMessage: String
        get() = _errorMessage
    val state: State
        get() = _state
    val volume: Int
        get() = _volume

    // Physical position (relative to screen) & dimensions of sound view for maximum quick access
    // TODO: Putting this on hold
    private var left: Float = 0f
    private var top: Float = 0f
    private var right: Float = 0f
    private var bottom: Float = 0f

    init {
        job = scope.launch { audioFile = createAudioFile() }
    }

    /********** PUBLIC METHODS **********/

    @Suppress("unused")
    fun isAtPosition(posX: Float, posY: Float): Boolean {
        return posX in left..right && posY in top..bottom
    }

    fun release() {
        _state = State.RELEASED
        audioFile?.release()
        scope.launch { stopTempPlayers() }
        stateListener = null
        durationListener = null
    }

    fun setBufferSize(value: Int) {
        if (value != bufferSize) {
            bufferSize = value
            audioFile?.changeBufferSize(value)
        }
    }

    fun setStateListener(listener: StateListener?) {
        stateListener = listener
    }

    fun setVolume(value: Int) {
        _volume = value
        audioFile?.changeVolume(value)
    }

    fun togglePlay() = scope.launch {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "togglePlay: state=$_state, repressMode=$repressMode, audioFile=$audioFile")
        when (_state) {
            State.PLAYING -> {
                val timeoutUs = System.nanoTime() + Constants.SOUND_PLAY_TIMEOUT
                when (repressMode) {
                    RepressMode.STOP -> {
                        audioFile?.stop()
                        stopTempPlayers()
                    }
                    RepressMode.RESTART -> audioFile?.restart(timeoutUs)
                    RepressMode.OVERLAP -> {
                        // TODO: adjust volumes?
                        audioFile?.also { makeTemporary(it) }
                        audioFile =
                            AudioFile(sound.path, sound.volume, bufferSize, this@SoundPlayer).prepare().play(timeoutUs)
                    }
                    RepressMode.PAUSE -> {
                        if (audioFile?.isPlaying == true) audioFile?.pause()
                        stopTempPlayers()
                    }
                }
            }
            State.PAUSED -> audioFile?.resume()
            else -> audioFile?.play()
        }
    }


    /********** OVERRIDDEN METHODS **********/

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is SoundPlayer && this.hashCode() == other.hashCode() && this.state == other.state -> true
            else -> false
        }
    }

    override fun hashCode() = sound.id.hashCode()

    override fun onAudioFileError(message: String) {
        _state = State.ERROR
        _errorMessage = message
    }

    override fun onAudioFileStateChange(state: AudioFile.State) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            AudioFile.State.INITIALIZING -> _state = State.INITIALIZING
            AudioFile.State.PLAYING -> _state = State.PLAYING
            AudioFile.State.PAUSED -> _state = State.PAUSED
            AudioFile.State.READY -> _state = State.READY
            AudioFile.State.STOPPED -> _state = State.STOPPED
        }
    }

    override fun onAudioFileWarning(message: String) {
        stateListener?.onSoundPlayerWarning(message)
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundPlayer $hashCode <sound=$sound, state=$_state>"
    }


    /********** PRIVATE METHODS **********/
    private suspend fun createAudioFile(): AudioFile? {
        return try {
            AudioFile(sound.path, sound.volume, bufferSize, this).prepareAndPrime().also { _duration = it.duration }
        } catch (e: Exception) {
            _errorMessage = "Error initializing ${sound.name}" + e.message?.let { ": $it" }
            _state = State.ERROR
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "Error initializing ${sound.name}: ${e.message}", e)
            null
        }
    }

    private suspend fun makeTemporary(audioFile: AudioFile) {
        if (audioFile.isPlaying) {
            audioFile.onStateChanged { state ->
                if (state == AudioFile.State.STOPPED) scope.launch {
                    audioFile.release()
                    tempAudioFileMutex.withLock {
                        if (BuildConfig.DEBUG)
                            Log.d(LOG_TAG, "Removing audioFile=$audioFile from tempAudioFiles=$tempAudioFiles")
                        tempAudioFiles.remove(audioFile)
                    }
                }
            }
            tempAudioFileMutex.withLock {
                if (BuildConfig.DEBUG)
                    Log.d(LOG_TAG, "Adding audioFile=$audioFile to tempAudioFiles=$tempAudioFiles")
                tempAudioFiles.add(audioFile)
            }
        }
    }

    private suspend fun stopTempPlayers() = tempAudioFileMutex.withLock {
        // This will also call release() on them via listener
        tempAudioFiles.forEach {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "Stopping audioFile=$it (in tempAudioFiles=$tempAudioFiles)")
            it.stop()
        }
    }


    /********** INNER CLASSES/INTERFACES/ENUMS **********/
    interface StateListener {
        fun onSoundPlayerStateChange(state: State, playbackPositionMs: Long): Any?
        fun onSoundPlayerWarning(message: String): Any?
    }

    interface DurationListener {
        fun onSoundPlayerDurationChange(sound: Sound, duration: Long)
    }

    enum class State { INITIALIZING, READY, STOPPED, PLAYING, PAUSED, ERROR, RELEASED }

    enum class RepressMode { STOP, RESTART, OVERLAP, PAUSE }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}