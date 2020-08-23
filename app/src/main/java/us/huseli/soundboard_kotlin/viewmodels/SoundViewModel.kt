package us.huseli.soundboard_kotlin.viewmodels

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

@Suppress("LocalVariableName")
class SoundViewModel(private val sound: Sound) : ViewModel() {
    /** Private fields */
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    /** Public fields */
    val player: Player by lazy { Player(GlobalApplication.application) }

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    /** Model fields */
    val id: Int? = sound.id

    val volume = liveData { emit(sound.volume) }

    val name = liveData { emit(sound.name) }

    val categoryId = liveData { emit(sound.categoryId) }


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