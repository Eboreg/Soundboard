package us.huseli.soundboard.audio

import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound

class SoundPlayer(val sound: Sound, private var bufferSize: Int) : AudioFile.StateListener {
    private var audioFile: AudioFile? = null
    private val tempAudioFiles = mutableListOf<AudioFile>()

    private var job: Job? = null
    private var listener: Listener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var _duration: Int = -1
    private var _errorMessage = ""
    private var _state = State.INITIALIZING
        set(value) {
            field = value
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "state change: this=$this, uri=$sound, onStateChangeListener=$listener, state=$value, listener=$listener")
            listener?.onSoundPlayerStateChange(this, state)
        }

    var repressMode = RepressMode.STOP
    var volume: Int = sound.volume
        set(value) {
            field = value
            audioFile?.setVolume(value)
        }

    val duration: Int
        get() = _duration
    val errorMessage: String
        get() = _errorMessage
    val state: State
        get() = _state

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

    @Suppress("unused")
    fun isAtPosition(posX: Float, posY: Float): Boolean {
        return posX in left..right && posY in top..bottom
    }

    private fun createAudioFile(): AudioFile? {
        return try {
            AudioFile(sound, bufferSize, this).prepare()
        } catch (e: AudioFile.AudioFileException) {
            _errorMessage = errorMessageFromType(e.errorType)
            _state = State.ERROR
            null
        } catch (e: Exception) {
            _errorMessage = "Error initializing ${sound.name}"
            _state = State.ERROR
            null
        }
    }

    private fun errorMessageFromType(errorType: AudioFile.Error): String {
        return when (errorType) {
            AudioFile.Error.BUILD_AUDIO_TRACK -> "Error building audio track"
            AudioFile.Error.CODEC -> "Codec error"
            AudioFile.Error.CODEC_GET_WRITE_OUTPUT_BUFFER -> "Error getting/writing codec output buffer"
            AudioFile.Error.CODEC_START -> "Error starting codec"
            AudioFile.Error.CODEC_WRONG_STATE -> "Wrong codec state"
            AudioFile.Error.GET_MEDIA_TYPE -> "Could not get media type"
            AudioFile.Error.GET_MIME_TYPE -> "Could not get MIME type"
            AudioFile.Error.NO_SUITABLE_CODEC -> "Could not find a suitable codec"
            AudioFile.Error.OUTPUT -> "Error outputting audio"
            AudioFile.Error.OUTPUT_BAD_VALUE -> "Audio output: bad value"
            AudioFile.Error.OUTPUT_DEAD_OBJECT -> "Audio output: dead object"
            AudioFile.Error.OUTPUT_NOT_PROPERLY_INITIALIZED -> "Audio output: not properly initialized"
            AudioFile.Error.TIMEOUT -> "Operation timed out"
        }
    }

    private fun isPlaying(): Boolean {
        /**
         * This once threw "Attempt to invoke virtual method 'boolean
         * us.huseli.soundboard.helpers.AudioFile.isPlaying()' on a null object reference". No
         * idea how that could happen, but might as well compensate for it.
         */
        return try {
            audioFile?.isPlaying == true || tempAudioFiles.any {
                try {
                    it.isPlaying
                } catch (e: NullPointerException) {
                    false
                }
            }
        } catch (e: NullPointerException) {
            false
        }
    }

    fun setBufferSize(value: Int) = runBlocking {
        if (value != bufferSize) {
            bufferSize = value
            _state = State.INITIALIZING
            job?.join()
            job = scope.launch { audioFile?.changeBufferSize(value) }
            // audioFile?.setBufferSize(value)
        }
    }

    fun togglePlay() {
        if (_state == State.PLAYING) {
            when (repressMode) {
                RepressMode.STOP -> {
                    _state = State.STOPPED
                    audioFile?.stop()?.prepare()
                    stopAndClearTempPlayers()
                }
                RepressMode.RESTART -> {
                    audioFile?.restart()
                }
                RepressMode.OVERLAP -> {
                    // TODO: adjust volumes?
                    createAndStartTempPlayer()
                }
            }
        } else {
            audioFile?.play()
        }
    }

    private fun createAndStartTempPlayer() {
        AudioFile(sound, bufferSize, TempAudioFileListener()).prepare().let {
            it.play()
            synchronized(this) { tempAudioFiles.add(it) }
        }
    }

    private fun stopAndClearTempPlayers() = synchronized(this) {
        // Stop and release them to be on the safe side
        tempAudioFiles.forEach { it.stop().release() }
        tempAudioFiles.clear()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
        if (BuildConfig.DEBUG) Log.i(
            LOG_TAG,
            "setListener: this=$this, uri=$sound, listener=$listener"
        )
    }

    fun reinit() = runBlocking {
        job?.join()
        job = scope.launch { audioFile?.reinit() }
    }

    fun release() = runBlocking {
        _state = State.RELEASED
        audioFile?.release()
        job?.join()
        stopAndClearTempPlayers()
        listener = null
    }

    override fun onError(errorType: AudioFile.Error) {
        _state = State.ERROR
        _errorMessage = errorMessageFromType(errorType)
    }

    override fun onInit() {
        audioFile?.also {
            _duration = it.duration.toInt()
            it.setVolume(volume)
            _state = State.READY
        }
    }

    override fun onPlay() {
        _state = State.PLAYING
    }

    override fun onReady() {
        if (!isPlaying()) {
            _state = State.READY
        }
    }

    override fun onReleased() {}

    override fun onStop(audioFile: AudioFile) {
        if (!isPlaying()) _state = State.STOPPED
    }

    override fun onWarning(errorType: AudioFile.Error) {
        listener?.onSoundPlayerWarning(errorMessageFromType(errorType))
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is SoundPlayer && this.hashCode() == other.hashCode() -> true
            else -> false
        }
    }

    override fun hashCode() = sound.id.hashCode()

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundPlayer $hashCode <sound=$sound, state=$state>"
    }

    inner class TempAudioFileListener : AudioFile.StateListener {
        override fun onError(errorType: AudioFile.Error) {}
        override fun onInit() {}
        override fun onReady() {}
        override fun onReleased() {}
        override fun onWarning(errorType: AudioFile.Error) {}

        override fun onPlay() {
            _state = State.PLAYING
        }

        override fun onStop(audioFile: AudioFile) {
            audioFile.release()
            synchronized(this) { tempAudioFiles.remove(audioFile) }
            if (!isPlaying()) _state = State.READY
        }
    }


    interface Listener {
        fun onSoundPlayerStateChange(player: SoundPlayer, state: State): Any?
        fun onSoundPlayerWarning(message: String): Any?
    }

    // Keeping STOPPED just because there may be a tiny time gap between a sound stopping and it
    // becoming READY again
    enum class State { INITIALIZING, READY, STOPPED, PLAYING, ERROR, RELEASED }

    enum class RepressMode { STOP, RESTART, OVERLAP }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}