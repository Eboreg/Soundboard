package us.huseli.soundboard_kotlin

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.pow
import kotlin.math.roundToInt

class SoundPlayer(private val context: Context, private val uri: Uri, private val volume: Int) {
    constructor(context: Context, uri: Uri) : this(context, uri, 100)

    private val mediaPlayer = MediaPlayer()

    var duration: Int = 0  // In seconds
    var isPlaying = false
    var isValid = true
    var errorMessage = ""

    fun setup() {
        try {
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            duration = (mediaPlayer.duration.toDouble() / 1000).roundToInt()
            setVolume(volume)
        } catch (e: Exception) {
            isValid = false
            duration = -1
            errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
        }
    }

    fun pause() {
        mediaPlayer.pause()
        isPlaying = false
        mediaPlayer.seekTo(0)
    }

    fun play() {
        mediaPlayer.start()
        isPlaying = true
    }

    fun setVolume(value: Int) {
        // MediaPlayer works with log values for some reason
        val volume = (100.0.pow((if(value <= 100) value else 100) / 100.0) / 100).toFloat()
        mediaPlayer.setVolume(volume, volume)
    }

    fun setOnCompletionListener(function: (MediaPlayer) -> Unit) {
        mediaPlayer.setOnCompletionListener(function)
    }

    fun release() = mediaPlayer.release()
}