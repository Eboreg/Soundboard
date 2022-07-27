package us.huseli.soundboard.audio

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound

class SoundPlayer(
    private val sound: Sound,
    private var bufferSize: Int,
    private var durationListener: DurationListener?
) : AudioFile.Listener {
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

    fun togglePlay(timeoutUs: Long) = scope.launch {
        when {
            repressMode == RepressMode.OVERLAP && (_state == State.PLAYING || tempAudioFiles.size > 0) -> {
                /**
                 * Not totally failsafe, but a decent compromise: If tempAudioFiles is empty and audioFile is not
                 * playing, we assume audioFile is the one to use. Otherwise, always create a new one and play it (we
                 * cannot rely on the current AudioFile's state, since it could be in a number of different states for
                 * timing reasons).
                 */
                // TODO: adjust volumes?
                Log.d(LOG_TAG,
                    "REPRESSTEST: toggleplay() before, state=$_state, tempAudioFiles.size=${tempAudioFiles.size}, timeoutUs=$timeoutUs, audioFile=$audioFile")
                try {
                    audioFile = audioFile?.let {
                        it.onStateChanged {}
                        makeTemporary(it)
                        it.copy(this@SoundPlayer).also { afCopy ->
                            Log.d(LOG_TAG,
                                "REPRESSTEST: toggleplay(), created COPY=$afCopy, state=$_state, tempAudioFiles.size=${tempAudioFiles.size}, timeoutUs=$timeoutUs")
                        }
                    } ?: AudioFile.create(sound.path, sound.volume, bufferSize, this@SoundPlayer).also {
                        Log.d(LOG_TAG,
                            "REPRESSTEST: toggleplay(), created NEW=$it, state=$_state, tempAudioFiles.size=${tempAudioFiles.size}, timeoutUs=$timeoutUs")
                    }
                    audioFile?.prepare()?.play(timeoutUs)
                } catch (e: Exception) {
                    _errorMessage = "Error playing ${sound.name}" + e.message?.let { ": $it" }
                    _state = State.ERROR
                    if (BuildConfig.DEBUG) Log.e(LOG_TAG, "Error initializing ${sound.name}: ${e.message}", e)
                }
                Log.d(LOG_TAG,
                    "REPRESSTEST: toggleplay() after, state=$_state, tempAudioFiles.size=${tempAudioFiles.size}, timeoutUs=$timeoutUs, audioFile=$audioFile")
            }
            _state == State.PLAYING -> {
                when (repressMode) {
                    RepressMode.STOP -> {
                        audioFile?.stop()
                        stopTempPlayers()
                    }
                    RepressMode.RESTART -> audioFile?.restart(timeoutUs)
                    RepressMode.PAUSE -> {
                        audioFile?.pause()
                        stopTempPlayers()
                    }
                    else -> {}
                }
            }
            _state == State.PAUSED -> audioFile?.resume()
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
        when (state) {
            AudioFile.State.INITIALIZING -> _state = State.INITIALIZING
            AudioFile.State.PLAYING -> _state = State.PLAYING
            AudioFile.State.PAUSED -> _state = State.PAUSED
            AudioFile.State.READY -> _state = State.READY
            AudioFile.State.STOPPED -> _state = State.STOPPED
            else -> {}
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
            AudioFile.create(sound.path, sound.volume, bufferSize, this).prepareAndPrime()
                .also { _duration = it.duration }
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
            audioFile.releaseAfterStop()
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
        fun onSoundPlayerDurationChange(sound: Sound, duration: Long): Any?
    }

    enum class State { INITIALIZING, READY, STOPPED, PLAYING, PAUSED, ERROR, RELEASED }

    enum class RepressMode { STOP, RESTART, OVERLAP, PAUSE }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}