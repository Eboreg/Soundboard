package us.huseli.soundboard_kotlin

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.IOException
import kotlin.math.pow

class SoundPlayer(private val context: Context, private val uri: Uri, private val volume: Int) {
    private var mediaPlayer: MediaPlayer? = null
    private var onStateChangeListener: OnStateChangeListener? = null
    private var _state = State.INITIALIZING
    private var _duration: Int = -1  // In milliseconds
    private var _errorMessage = ""
    private var _noPermission = false

    val duration: Int
        get() = _duration
    val errorMessage: String
        get() = _errorMessage
    val state: State
        get() = _state
    val noPermission: Boolean
        get() = _noPermission

    fun setup() {
        try {
            mediaPlayer = MediaPlayer().also { mediaPlayer ->
                mediaPlayer.setDataSource(context, uri)
                mediaPlayer.prepare()
                _duration = mediaPlayer.duration
                setVolume(volume)
                mediaPlayer.setOnCompletionListener { stop() }
                changeState(State.READY)
            }
        } catch (e: Exception) {
            if (e is IOException) _noPermission = true
            _duration = -1
            _errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
            changeState(State.ERROR)
        }
    }

    private fun changeState(state: State) {
        if (state != _state) {
            _state = state
            onStateChangeListener?.onSoundPlayerStateChange(this, state)
        }
    }

    private fun stop() {
        mediaPlayer?.let {
            it.pause()
            changeState(State.STOPPED)
            it.seekTo(0)
        }
    }

    private fun play() {
        mediaPlayer?.let {
            it.start()
            changeState(State.PLAYING)
        }
    }

    fun togglePlay() {
        if (_state == State.PLAYING) stop() else play()
    }

    fun setVolume(value: Int) {
        // MediaPlayer works with log values for some reason
        mediaPlayer?.let {
            val volume = (100.0.pow((if (value <= 100) value else 100) / 100.0) / 100).toFloat()
            it.setVolume(volume, volume)
        }
    }

    fun setOnStateChangeListener(listener: OnStateChangeListener) {
        onStateChangeListener = listener
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }


    interface OnStateChangeListener {
        fun onSoundPlayerStateChange(player: SoundPlayer, state: State): Any?
    }


    enum class State {
        INITIALIZING,
        READY,
        STOPPED,
        PLAYING,
        ERROR
    }
}