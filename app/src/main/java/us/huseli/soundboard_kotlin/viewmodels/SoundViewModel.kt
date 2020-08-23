package us.huseli.soundboard_kotlin.viewmodels

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundDatabase
import us.huseli.soundboard_kotlin.data.SoundRepository

@Suppress("LocalVariableName")
class SoundViewModel(private val sound: Sound) : ViewModel() {
    /** Private fields */
    private val repository = SoundRepository(SoundDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())

    /** Public fields */
    val player: Player by lazy { Player(GlobalApplication.application) }

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    /** Model fields */
    val id: Int? = sound.id

    val volume = liveData { emit(sound.volume) }
    fun setVolume(value: Int) {
        sound.volume = value
        player.setVolume(value)
    }

    val name = liveData { emit(sound.name) }
    fun setName(value: String) {
        if (value.trim().isNotEmpty()) sound.name = value.trim()
    }

    val categoryId = liveData { emit(sound.categoryId) }
    fun setCategoryId(value: Int) {
        sound.categoryId = value
    }

    /** Methods */
    fun save() {
        when (sound.id) {
            null -> insert()
            else -> update()
        }
    }

    fun delete() = viewModelScope.launch(Dispatchers.IO) {
        sound.id?.let { id -> repository.delete(id) }
    }

    private fun insert() = viewModelScope.launch(Dispatchers.IO) { repository.insert(sound) }
    private fun update() = viewModelScope.launch(Dispatchers.IO) { repository.update(sound) }


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

        fun setVolume(value: Int) = mediaPlayer.setVolume(value.toFloat() / 100, value.toFloat() / 100)
   }
}