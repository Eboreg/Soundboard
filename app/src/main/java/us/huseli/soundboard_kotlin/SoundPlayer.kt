package us.huseli.soundboard_kotlin

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

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

    fun setVolume(value: Int) = mediaPlayer.setVolume(value.toFloat() / 100, value.toFloat() / 100)

    fun setOnCompletionListener(function: () -> Unit) {
        mediaPlayer.setOnCompletionListener { function() }
    }
}