package us.huseli.soundboard

import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.AudioFile

class SoundPlayer(private val sound: Sound, val path: String, private var bufferSize: Int) {
    private var audioFile: AudioFile?
    private val tempAudioFiles = mutableListOf<AudioFile>()

    private var listener: Listener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var _duration: Int = -1
    private var _errorMessage = ""
    private var _state = State.INITIALIZING
        set(value) {
            field = value
            Log.d(LOG_TAG, "state change: this=$this, uri=$sound, onStateChangeListener=$listener, state=$value")
            listener?.onSoundPlayerStateChange(this, state)
        }

    var repressMode = RepressMode.STOP
    var volume: Int? = null
        set(value) {
            if (value != null) {
                field = value
                audioFile?.setVolume(value)
            }
        }

    val duration: Int
        get() = _duration
    val errorMessage: String
        get() = _errorMessage
    val state: State
        get() = _state

    init {
        Log.i(LOG_TAG, "init: uri=$sound, path=$path")
        audioFile = createAudioFile()
        volume = sound.volume
    }

    private fun createAudioFile(): AudioFile? {
        return try {
            AudioFile(path, sound.name, bufferSize) {
                _duration = it.duration.toInt()
                _state = State.READY
            }.setOnReadyListener {
                if (!isPlaying()) _state = State.READY
            }.setOnStopListener {
                if (!isPlaying()) _state = State.STOPPED
            }.setOnErrorListener { _, errorType ->
                _state = State.ERROR
                _errorMessage = errorMessageFromType(errorType)
            }.setOnWarningListener { _, errorType ->
                listener?.onSoundPlayerWarning(errorMessageFromType(errorType))
            }.setOnPlayListener {
                _state = State.PLAYING
            }
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

    private fun isPlaying(): Boolean =
            audioFile?.isPlaying == true || tempAudioFiles.any { it.isPlaying }

    fun setBufferSize(value: Int) = scope.launch {
        if (value != bufferSize) {
            bufferSize = value
            audioFile?.release()
            audioFile = createAudioFile()
            // audioFile?.setBufferSize(value)
        }
    }

    fun togglePlay() {
        if (_state == State.PLAYING) {
            when (repressMode) {
                RepressMode.STOP -> {
                    _state = State.STOPPED
                    audioFile?.stop()
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
        AudioFile(path, sound.name, bufferSize).let {
            it.play()
            tempAudioFiles.add(it)
            it.setOnPlayListener {
                _state = State.PLAYING
            }
            it.setOnStopListener { audioFile ->
                audioFile.release()
                tempAudioFiles.remove(audioFile)
                if (!isPlaying()) _state = State.READY
            }
        }
    }

    private fun stopAndClearTempPlayers() {
        tempAudioFiles.forEach {
            it.stop()
            it.release()
        }
        tempAudioFiles.clear()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
        Log.i(LOG_TAG, "setListener: this=$this, uri=$sound, listener=$listener")
    }

    fun release() {
        scope.cancel()
        stopAndClearTempPlayers()
        audioFile?.release()
        listener = null
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is SoundPlayer && this.hashCode() == other.hashCode() -> true
            else -> false
        }
    }

    override fun hashCode() = sound.id.hashCode()


    interface Listener {
        fun onSoundPlayerStateChange(player: SoundPlayer, state: State): Any?
        fun onSoundPlayerWarning(message: String): Any?
    }

    // Keeping STOPPED just because there may be a tiny time gap between a sound stopping and it
    // becoming READY again
    enum class State { INITIALIZING, READY, STOPPED, PLAYING, ERROR, }

    enum class RepressMode { STOP, RESTART, OVERLAP }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}