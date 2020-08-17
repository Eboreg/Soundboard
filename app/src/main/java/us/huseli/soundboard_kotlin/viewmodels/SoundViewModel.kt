package us.huseli.soundboard_kotlin.viewmodels

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
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

        val isPlaying
            get() = mediaPlayer.isPlaying
        var isValid = true
        var errorMessage = ""

        init {
            try {
                mediaPlayer.setDataSource(context, sound.uri)
                mediaPlayer.prepare()
                setVolume(sound.volume)
            } catch (e: Exception) {
                isValid = false
                errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
            }
        }

        fun pause() {
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        }

        fun play() = mediaPlayer.start()

        fun setVolume(value: Int) = mediaPlayer.setVolume(value.toFloat() / 100, value.toFloat() / 100)

        fun setOnCompletionListener(function: () -> Unit) = mediaPlayer.setOnCompletionListener { function() }
    }
}