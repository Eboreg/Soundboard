package us.huseli.soundboard_kotlin

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlin.math.pow

class SoundPlayer(context: Context, uri: Uri, volume: Int) {
    private val mediaPlayer = MediaPlayer()

    var isPlaying = false
    var isValid = true
    var errorMessage = ""

    init {
        try {
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            setVolume(volume)
        } catch (e: Exception) {
            isValid = false
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
}