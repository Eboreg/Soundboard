package us.huseli.soundboard_kotlin

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SoundPlayer(context: Context, uri: Uri, volume: Int) {
    private val mediaPlayer = MediaPlayer()
    private val _isPlaying = MutableLiveData(false)
    private val _isValid = MutableLiveData(true)

    val isPlaying: LiveData<Boolean>
        get() = _isPlaying
    val isValid: LiveData<Boolean>
        get() = _isValid
    var errorMessage = ""

    init {
        try {
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            mediaPlayer.setOnCompletionListener { pause() }
            setVolume(volume)
        } catch (e: Exception) {
            _isValid.value = false
            errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
        }
    }

    private fun pause() {
        mediaPlayer.pause()
        _isPlaying.value = false
        mediaPlayer.seekTo(0)
    }

    private fun play() {
        mediaPlayer.start()
        _isPlaying.value = true
    }

    fun playOrPause() = if (mediaPlayer.isPlaying) pause() else play()

    fun setVolume(value: Int) = mediaPlayer.setVolume(value.toFloat() / 100, value.toFloat() / 100)
}