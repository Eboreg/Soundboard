package us.huseli.soundboard

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.pow

class SoundPlayer(private var context: Context?, private val uri: Uri, initVolume: Int) :
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private var mediaPlayer: MediaPlayer? = null
    private val tempMediaPlayers = mutableListOf<MediaPlayer>()

    private var onStateChangeListeners = mutableSetOf<OnStateChangeListener>()
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private var _state = State.INITIALIZING
    private var _duration: Int = -1  // In milliseconds
    private var _errorMessage = ""
    private var _noPermission = false
    private var _previousVolume: Int? = null

    var repressMode = RepressMode.STOP
    var volume: Int = initVolume
        set(value) {
            if (value != _previousVolume) {
                field = value
                _previousVolume = value
                // MediaPlayer works with log values for some reason
                val mpVolume = (100.0.pow((if (value <= 100) value else 100) / 100.0) / 100).toFloat()
                mediaPlayer?.setVolume(mpVolume, mpVolume)
            }
        }

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
            Log.i(LOG_TAG, "init: uri=$uri, volume=$initVolume")
            mediaPlayer = MediaPlayer().also { mediaPlayer ->
                mediaPlayer.setOnPreparedListener(this@SoundPlayer)
                mediaPlayer.setOnErrorListener(this@SoundPlayer)
                try {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    mediaPlayer.setDataSource(context!!, uri)
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

    private fun isPlaying() = mediaPlayer?.isPlaying == true || tempMediaPlayers.any { it.isPlaying }

    private fun changeState(state: State) {
        Log.i(LOG_TAG, "changeState: this=$this, uri=$uri, onStateChangeListeners=$onStateChangeListeners, state=$state")
        _state = state
        onStateChangeListeners.forEach { it.onSoundPlayerStateChange(this, state) }
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
                mediaPlayer.start()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    changeState(State.PLAYING)
                }
            }
        }
    }

    private fun createAndStartTempPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            context?.let { context ->
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

    private fun stopAndClearTempPlayers() {
        tempMediaPlayers.forEach {
            it.stop()
            it.release()
        }
        tempMediaPlayers.clear()
    }

    fun addOnStateChangeListener(listener: OnStateChangeListener) {
        if (listener !in onStateChangeListeners) onStateChangeListeners.add(listener)
        Log.i(LOG_TAG, "addOnStateChangeListener: this=$this, uri=$uri, listener=$listener, onStateChangeListeners=$onStateChangeListeners")
    }

    fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        onStateChangeListeners.remove(listener)
        Log.i(LOG_TAG, "removeOnStateChangeListener: this=$this, uri=$uri, listener=$listener, onStateChangeListeners=$onStateChangeListeners")
    }

    fun release() {
        scope.cancel()
        context = null
        stopAndClearTempPlayers()
        mediaPlayer?.release()
        onStateChangeListeners.clear()
    }


    interface OnStateChangeListener {
        fun onSoundPlayerStateChange(player: SoundPlayer, state: State): Any?
    }


    enum class State { INITIALIZING, READY, STOPPED, PLAYING, ERROR }

    enum class RepressMode { STOP, RESTART, OVERLAP }

    companion object {
        const val LOG_TAG = "SoundPlayer"
    }
}