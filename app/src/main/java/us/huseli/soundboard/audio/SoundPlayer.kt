package us.huseli.soundboard.audio

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Sound

class SoundPlayer(private var sound: Sound,
                  private var bufferSize: Int,
                  private var durationListener: DurationListener?) : AudioFile.Listener {
    private var _duration: Int = -1
        set(value) {
            if (value != field) {
                field = value
                if (value > -1) durationListener?.onSoundPlayerDurationChange(sound, value)
            }
        }
    private var _errorMessage = ""
    private var _state = State.INITIALIZING
        set(value) {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "state change: this=$this, uri=$sound, onStateChangeListener=$stateListener, state=$value, oldState = $field, listener=$stateListener")
            stateListener?.onSoundPlayerStateChange(this, value, field)
            field = value
        }
    private var _volume = sound.volume
    private var audioFile: AudioFile? = null
    private var job: Job? = null
    private var stateListener: StateListener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val tempAudioFiles = mutableListOf<AudioFile>()
    private val tempAudioFileMutex = Mutex()

    var repressMode = RepressMode.STOP
        set(value) {
            if (value != field && field == RepressMode.PAUSE && _state == State.PAUSED) {
                // Mode was PAUSE and is not anymore, meaning we "unpause" any paused sound
                audioFile?.prepareAndPrime()
            }
            field = value
        }

    val duration: Int
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
        if (BuildConfig.DEBUG) Log.i(LOG_TAG, "init: uri=$sound, path=${sound.path}")
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
        scope.launch { stopAndClearTempPlayers() }
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
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "setListener: this=$this, uri=$sound, listener=$listener")
    }

    fun setVolume(value: Int) {
        _volume = value
        audioFile?.volume = value
    }

    fun togglePlay() {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "togglePlay: state=$_state, repressMode=$repressMode, audioFile=$audioFile")
        when (_state) {
            State.PLAYING -> {
                val timeoutUs = System.nanoTime() + Constants.SOUND_PLAY_TIMEOUT
                scope.launch {
                    when (repressMode) {
                        RepressMode.STOP -> {
                            audioFile?.stopAndPrepare()
                            stopAndClearTempPlayers()
                        }
                        RepressMode.RESTART -> audioFile?.restartAndPrepare(timeoutUs)
                        // TODO: adjust volumes?
                        RepressMode.OVERLAP -> createAndStartTempPlayer(timeoutUs)
                        RepressMode.PAUSE -> {
                            if (audioFile?.isPlaying == true) audioFile?.pause()
                            stopAndClearTempPlayers()
                        }
                    }
                }
            }
            State.PAUSED -> audioFile?.resumeAndPrepare()
            else -> audioFile?.playAndPrepare()
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

    override fun onAudioFileStateChange(state: AudioFile.State, audioFile: AudioFile, message: String?) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            AudioFile.State.INITIALIZING -> this._state = State.INITIALIZING
            AudioFile.State.PLAYING -> this._state = State.PLAYING
            AudioFile.State.READY -> scope.launch { setReadyOnTempPlayersStopped() }
            AudioFile.State.STOPPED -> if (!isPlaying()) this._state = State.STOPPED
            AudioFile.State.PAUSED -> this._state = State.PAUSED
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

    private suspend fun createAndStartTempPlayer(timeoutUs: Long) {
        AudioFile(sound, _volume, bufferSize, TempAudioFileListener()).prepare().let {
            it.play(timeoutUs)
            tempAudioFileMutex.withLock { tempAudioFiles.add(it) }
        }
    }

    private fun createAudioFile(): AudioFile? {
        return try {
            AudioFile(sound, bufferSize, this).prepareAndPrime().also { _duration = it.duration.toInt() }
        } catch (e: AudioFile.AudioFileException) {
            _errorMessage = e.message
            _state = State.ERROR
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "Error initializing ${sound.name}: ${e.message}", e)
            null
        } catch (e: Exception) {
            _errorMessage = "Error initializing ${sound.name}"
            _state = State.ERROR
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "Error initializing ${sound.name}", e)
            null
        }
    }

    private fun isPlaying(): Boolean = runBlocking {
        tempAudioFileMutex.withLock { (audioFile?.isPlaying == true || tempAudioFiles.any { it.isPlaying }) }
    }

    private suspend fun stopAndClearTempPlayers() = tempAudioFileMutex.withLock {
        // This will also call release() on them via listener below
        tempAudioFiles.forEach { it.stop() }
        tempAudioFiles.clear()
    }

    private suspend fun setReadyOnTempPlayersStopped() {
        while (isPlaying()) delay(50)
        _state = State.READY
    }


    /********** INNER CLASSES/INTERFACES/ENUMS **********/

    inner class TempAudioFileListener : AudioFile.Listener {
        override fun onAudioFileStateChange(state: AudioFile.State, audioFile: AudioFile, message: String?) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (state) {
                AudioFile.State.PLAYING -> _state = State.PLAYING
                AudioFile.State.STOPPED -> scope.launch {
                    audioFile.release()
                    /**
                     * If state is already STOPPED, it means the user has stopped playback manually. In this case,
                     * togglePlay() will probably be in the process of looping through tempAudioFiles, and so we
                     * shouldn't tamper with it, but instead clear the whole list in togglePlay() once it's done
                     * looping. If state is RELEASED, it's basically the same thing. Leave the loop alone.
                     */
                    if (_state != State.STOPPED && _state != State.RELEASED)
                        tempAudioFileMutex.withLock { tempAudioFiles.remove(audioFile) }
                }
            }
        }

        override fun onAudioFileError(message: String) {}
        override fun onAudioFileWarning(message: String) {}
    }


    interface StateListener {
        fun onSoundPlayerStateChange(player: SoundPlayer, state: State, oldState: State? = null): Any?
        fun onSoundPlayerWarning(message: String): Any?
    }

    interface DurationListener {
        fun onSoundPlayerDurationChange(sound: Sound, duration: Int)
    }

    // Keeping STOPPED just because there may be a tiny time gap between a sound stopping and it
    // becoming READY again
    enum class State { INITIALIZING, READY, STOPPED, PLAYING, PAUSED, ERROR, RELEASED }

    enum class RepressMode { STOP, RESTART, OVERLAP, PAUSE }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}