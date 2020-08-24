package us.huseli.soundboard_kotlin.viewmodels

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound

@Suppress("LocalVariableName")
class SoundViewModel(private val sound: Sound) : ViewModel() {
    /** Public fields */
    val player: Player by lazy { Player(GlobalApplication.application) }

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    /** Model fields */
    val id: Int? = sound.id

    val name = sound.name

    val categoryId = sound.categoryId


    inner class Player(context: Context) {
        private val mediaPlayer = MediaPlayer()

        var isValid = true
        var errorMessage = ""

        init {
            try {
                mediaPlayer.setDataSource(context, sound.uri)
                mediaPlayer.prepare()
                mediaPlayer.setOnCompletionListener { pause() }
                setVolume(sound.volume)
            } catch (e: Exception) {
                isValid = false
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

        private fun setVolume(value: Int) = mediaPlayer.setVolume(value.toFloat() / 100, value.toFloat() / 100)
   }
}