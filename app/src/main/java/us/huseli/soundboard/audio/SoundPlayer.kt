package us.huseli.soundboard.audio

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound
import java.util.*

class SoundPlayer(val sound: Sound, private var bufferSize: Int) : AudioFile.Listener {
    private val _tempAudioFiles = mutableListOf<AudioFile>()
    private var audioFile: AudioFile? = null
    private val tempAudioFiles = Collections.synchronizedList(_tempAudioFiles)

    private var job: Job? = null
    private var listener: Listener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var _duration: Int = -1
        set(value) {
            if (value != field) {
                field = value
                if (value > -1) listener?.onSoundPlayerDurationChange(value)
            }
        }
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
            AudioFile(sound, bufferSize, this).prepare().also { _duration = it.duration.toInt() }
        } catch (e: AudioFile.AudioFileException) {
            _errorMessage = e.message
            _state = State.ERROR
            Log.e(LOG_TAG, "Error initializing ${sound.name}: ${e.message}", e)
            null
        } catch (e: Exception) {
            _errorMessage = "Error initializing ${sound.name}"
            _state = State.ERROR
            Log.e(LOG_TAG, "Error initializing ${sound.name}", e)
            null
        }
    }

    private fun isPlaying(): Boolean {
        return synchronized(tempAudioFiles) {
            audioFile?.isPlaying == true || tempAudioFiles.any { it.isPlaying }
        }
    }

    suspend fun setBufferSize(value: Int) {
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
                    audioFile?.stop()
                    synchronized(tempAudioFiles) {
                        tempAudioFiles.forEach { it.stop() }
                    }
                    tempAudioFiles.clear()
                    audioFile?.prepare()
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
            tempAudioFiles.add(it)
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
        if (BuildConfig.DEBUG) Log.i(
            LOG_TAG,
            "setListener: this=$this, uri=$sound, listener=$listener"
        )
    }

    suspend fun reinit() {
        job?.join()
        job = scope.launch { audioFile?.reinit() }
    }

    suspend fun release() {
        _state = State.RELEASED
        audioFile?.release()
        job?.join()
        synchronized(tempAudioFiles) {
            // This will also call release() on them via listener below
            tempAudioFiles.forEach { it.stop() }
        }
        tempAudioFiles.clear()
        listener = null
    }

    override fun onAudioFileStateChange(state: AudioFile.State, audioFile: AudioFile, message: String?) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            AudioFile.State.INITIALIZING -> _state = State.INITIALIZING
            AudioFile.State.PLAYING -> _state = State.PLAYING
            AudioFile.State.READY -> if (!isPlaying()) _state = State.READY
            AudioFile.State.STOPPED -> {
                if (!isPlaying()) _state = State.STOPPED
                audioFile.prepare()
            }
        }
    }

    override fun onAudioFileError(message: String) {
        _state = State.ERROR
        _errorMessage = message
    }

    override fun onAudioFileWarning(message: String) {
        listener?.onSoundPlayerWarning(message)
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

    inner class TempAudioFileListener : AudioFile.Listener {
        override fun onAudioFileStateChange(state: AudioFile.State, audioFile: AudioFile, message: String?) {
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (state) {
                AudioFile.State.PLAYING -> _state = State.PLAYING
                AudioFile.State.STOPPED -> {
                    audioFile.release()
                    /**
                     * If state is already STOPPED, it means the user has stopped playback manually. In this case, togglePlay()
                     * will probably be in the process of looping through tempAudioFiles, and so we shouldn't tamper with it,
                     * but instead clear the whole list in togglePlay() once it's done looping.
                     * If state is RELEASED, it's basically the same thing. Leave the loop alone.
                     */
                    if (_state != State.STOPPED && _state != State.RELEASED) tempAudioFiles.remove(audioFile)
                    if (!isPlaying()) _state = State.READY
                }
            }
        }

        override fun onAudioFileError(message: String) {}
        override fun onAudioFileWarning(message: String) {}
    }


    interface Listener {
        fun onSoundPlayerDurationChange(duration: Int)
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