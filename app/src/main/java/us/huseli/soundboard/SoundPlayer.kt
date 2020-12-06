package us.huseli.soundboard

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.*
import kotlin.math.pow

class SoundPlayer(private val context: Context, private val uri: Uri, private val volume: Int) :
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private val mediaPlayer = MediaPlayer()
    private val tempMediaPlayers = mutableListOf<MediaPlayer>()

    private var onStateChangeListener: OnStateChangeListener? = null
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var _state = State.INITIALIZING
    private var _duration: Int = -1  // In milliseconds
    private var _errorMessage = ""
    private var _noPermission = false

    var repressMode = RepressMode.STOP

    val duration: Int
        get() = _duration
    val errorMessage: String
        get() = _errorMessage
    val state: State
        get() = _state
    val noPermission: Boolean
        get() = _noPermission

    init {
        scope.launch {
            mediaPlayer.setOnPreparedListener(this@SoundPlayer)
            mediaPlayer.setOnErrorListener(this@SoundPlayer)
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                mediaPlayer.setDataSource(context, uri)
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

    override fun onPrepared(mp: MediaPlayer) {
        _duration = mp.duration
        setVolume(volume)
        mp.setOnCompletionListener {
            if (!isPlaying()) changeState(State.READY)
            mediaPlayer.seekTo(0)
        }
        changeState(State.READY)
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (extra == MediaPlayer.MEDIA_ERROR_IO) _noPermission = true
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

    private fun isPlaying() = mediaPlayer.isPlaying || tempMediaPlayers.any { it.isPlaying }

    private fun changeState(state: State) {
        _state = state
        onStateChangeListener?.onSoundPlayerStateChange(this, state)
    }

    fun togglePlay() {
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
                    stopAndClearTempPlayers()
                    //changeState(State.PLAYING)
                    // TODO: Maybe remove, experimenting with "discontinuity listener"
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        changeState(State.PLAYING)
                    }
                }
                RepressMode.OVERLAP -> {
                    // TODO: adjust volumes?
                    MediaPlayer().apply {
                        setDataSource(context, uri)
                        prepare()
                        start()
                        tempMediaPlayers.add(this)
                        setOnCompletionListener {
                            release()
                            tempMediaPlayers.remove(this)
                            if (!this@SoundPlayer.isPlaying()) changeState(State.READY)
                        }
                        changeState(State.PLAYING)
                    }
                }
            }
        } else {
            mediaPlayer.start()
            //changeState(State.PLAYING)
            // TODO: Maybe remove, experimenting with "discontinuity listener"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                changeState(State.PLAYING)
            }
        }
    }

    private fun stopAndClearTempPlayers() {
        tempMediaPlayers.forEach {
            it.stop()
            it.release()
        }
        tempMediaPlayers.clear()
    }

    fun setVolume(value: Int) {
        // MediaPlayer works with log values for some reason
        val volume = (100.0.pow((if (value <= 100) value else 100) / 100.0) / 100).toFloat()
        mediaPlayer.setVolume(volume, volume)
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener) {
        onStateChangeListener = listener
    }

    fun release() = mediaPlayer.release()


    interface OnStateChangeListener {
        fun onSoundPlayerStateChange(player: SoundPlayer, state: State): Any?
    }


    enum class State { INITIALIZING, READY, STOPPED, PLAYING, ERROR }

    enum class RepressMode { STOP, RESTART, OVERLAP }
}