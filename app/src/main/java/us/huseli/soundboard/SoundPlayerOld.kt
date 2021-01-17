package us.huseli.soundboard

import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.AudioFile
import kotlin.math.pow

class SoundPlayerOld(private val sound: Sound) :
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private var audioFile: AudioFile? = null
    private var mediaPlayer: MediaPlayer? = null
    private val tempMediaPlayers = mutableListOf<MediaPlayer>()

    private var onStateChangeListener: OnStateChangeListener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var _state = State.INITIALIZING
    private var _duration: Int = -1  // In milliseconds
    private var _errorMessage = ""
    private var _previousVolume: Int? = null

    var repressMode = RepressMode.STOP
    var volume: Int? = null
        set(value) {
            if (value != null && value != _previousVolume) {
                field = value
                mediaPlayer?.let {
                    _previousVolume = value
                    // MediaPlayer works with log values for some reason
                    val mpVolume = (100.0.pow((if (value <= 100) value else 100) / 100.0) / 100).toFloat()
                    it.setVolume(mpVolume, mpVolume)
                }
            }
        }

    val duration: Int
        get() = _duration
    val errorMessage: String
        get() = _errorMessage
    val state: State
        get() = _state

    init {
        scope.launch {
            Log.i(LOG_TAG, "init: uri=$sound")

            sound.uri.path?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFile = AudioFile(it, sound.name)
                }
            }

            mediaPlayer = MediaPlayer().also { mediaPlayer ->
                mediaPlayer.setOnPreparedListener(this@SoundPlayerOld)
                mediaPlayer.setOnErrorListener(this@SoundPlayerOld)
                // Because mediaPlayer was null at object creation:
                volume = sound.volume
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    mediaPlayer.setDataSource(sound.uri.path)
                    //mediaPlayer.setDataSource(context!!, uri)
                    mediaPlayer.prepareAsync()
                } catch (e: Exception) {
                    _errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
                    changeState(State.ERROR)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mediaPlayer.setOnMediaTimeDiscontinuityListener { _, mts ->
                        if (mts.mediaClockRate > 0.0) changeState(State.PLAYING)
                    }
                }
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        _duration = mp.duration
        mp.setOnCompletionListener {
            if (!isPlaying()) changeState(State.READY)
            it.seekTo(0)
            //mediaPlayer.seekTo(0)
        }
        changeState(State.READY)
    }

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

    private fun isPlaying() = mediaPlayer?.isPlaying == true || tempMediaPlayers.any { it.isPlaying }

    private fun changeState(state: State) {
        Log.d(LOG_TAG, "changeState: this=$this, uri=$sound, onStateChangeListeners=$onStateChangeListener, state=$state")
        _state = state
        onStateChangeListener?.onSoundPlayerStateChange(this, state)
    }

    fun togglePlay() {
        mediaPlayer?.also { mediaPlayer ->
            if (_state == State.PLAYING) {
                when (repressMode) {
                    RepressMode.STOP -> {
                        mediaPlayer.pause()
                        stopAndClearTempPlayers()
                        changeState(State.STOPPED)
                        mediaPlayer.seekTo(0)
                    }
                    RepressMode.RESTART -> {
                        mediaPlayer.seekTo(0)
                        changeState(State.PLAYING)
                    }
                    RepressMode.OVERLAP -> {
                        // TODO: adjust volumes?
                        createAndStartTempPlayer()
                    }
                }
            } else {
                sound.uri.path?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioFile?.play()
                    }
                }
//                mediaPlayer.start()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    changeState(State.PLAYING)
                }
            }
        }
    }

    private fun createAndStartTempPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            setDataSource(sound.uri.path)
            prepare()
            start()
            tempMediaPlayers.add(this)
            setOnCompletionListener {
                release()
                tempMediaPlayers.remove(this)
                if (!this@SoundPlayerOld.isPlaying()) changeState(State.READY)
            }
            changeState(State.PLAYING)
        }
    }

    private fun stopAndClearTempPlayers() {
        tempMediaPlayers.forEach {
            it.stop()
            it.release()
        }
        tempMediaPlayers.clear()
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener) {
        onStateChangeListener = listener
        Log.i(LOG_TAG, "addOnStateChangeListener: this=$this, uri=$sound, listener=$listener")
    }

    fun removeOnStateChangeListener() {
        onStateChangeListener = null
        Log.i(LOG_TAG, "removeOnStateChangeListener: this=$this, uri=$sound")
    }

    fun release() {
        scope.cancel()
        stopAndClearTempPlayers()
        mediaPlayer?.release()
        onStateChangeListener = null
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is SoundPlayerOld && this.hashCode() == other.hashCode() -> true
            else -> false
        }
    }

    override fun hashCode() = sound.id.hashCode()


    interface OnStateChangeListener {
        fun onSoundPlayerStateChange(player: SoundPlayerOld, state: State): Any?
    }

    enum class State { INITIALIZING, READY, STOPPED, PLAYING, ERROR }

    enum class RepressMode { STOP, RESTART, OVERLAP }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}