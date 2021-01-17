package us.huseli.soundboard

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.AudioFile

class SoundPlayer(private val sound: Sound, val path: String) {
    private val audioFile: AudioFile?
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
        audioFile = try {
            AudioFile(path, sound.name) {
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
        volume = sound.volume
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
        }
    }

/*
    override fun onPrepared(mp: MediaPlayer) {
        _duration = mp.duration
        mp.setOnCompletionListener {
            if (!isPlaying()) changeState(State.READY)
            it.seekTo(0)
            //mediaPlayer.seekTo(0)
        }
        changeState(State.READY)
    }
*/

/*
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        _errorMessage = when (extra) {
            MediaPlayer.MEDIA_ERROR_IO -> "IO error"
            MediaPlayer.MEDIA_ERROR_MALFORMED -> "Malformed bitstream"
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "Unsupported format"
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Timeout"
            else -> "Unknown error"
        }
        changeState(State.ERROR)
        return true
    }
*/

    private fun isPlaying(): Boolean =
            audioFile?.isPlaying == true || tempAudioFiles.any { it.isPlaying }

    // private fun isPlaying() = mediaPlayer?.isPlaying == true || tempMediaPlayers.any { it.isPlaying }

    fun togglePlay() {
        if (_state == State.PLAYING) {
            when (repressMode) {
                RepressMode.STOP -> {
                    _state = State.STOPPED
                    audioFile?.stop()
                    stopAndClearTempPlayers()
                    //mediaPlayer.seekTo(0)
                }
                RepressMode.RESTART -> {
                    audioFile?.restart()
                    // _state = State.PLAYING
                }
                RepressMode.OVERLAP -> {
                    // TODO: adjust volumes?
                    createAndStartTempPlayer()
                }
            }
        } else {
            audioFile?.play()
            // mediaPlayer.start()
            // _state = State.PLAYING
        }
    }

    private fun createAndStartTempPlayer() {
        AudioFile(path, sound.name).let {
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