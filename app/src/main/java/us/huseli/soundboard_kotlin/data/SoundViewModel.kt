package us.huseli.soundboard_kotlin.data

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundViewModel : ViewModel() {
    // Private fields
    private lateinit var sound: Sound
    private lateinit var repository: SoundRepository
    private var mediaPlayer = MediaPlayer()

    // Model fields
    var name: String = ""
        set(value) {
            field = value.trim()
            sound.name = field
        }
    var id: Int? = null
    // Volume = 0 - 100 here, although MediaPlayer uses 0.0F - 1.0F internally
    var volume: Int = 100
        set(value) {
            field = value
            sound.volume = field
            mediaPlayer.setVolume(field.toFloat() / 100, field.toFloat() / 100)
        }

    var errorMessage: String = ""
    var isValid: Boolean = true
    val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    fun play() = mediaPlayer.start()

    fun pause() {
        mediaPlayer.apply {
            pause()
            seekTo(0)
        }
    }

    fun save() {
        when (id) {
            null -> insert()
            else -> update()
        }
    }

    fun setOnCompletionListener(function: () -> Unit) {
        mediaPlayer.setOnCompletionListener { function() }
    }

    private fun insert() = viewModelScope.launch(Dispatchers.IO) { repository.insert(sound) }

    private fun update() = viewModelScope.launch(Dispatchers.IO) { repository.update(sound) }

    companion object {
        fun getInstance(context: Context, sound: Sound): SoundViewModel {
            return SoundViewModel().apply {
                this.sound = sound
                repository = SoundRepository.getInstance(context)
                name = sound.name
                id = sound.id
                volume = sound.volume
                try {
                    mediaPlayer.setDataSource(context, sound.uri)
                    mediaPlayer.prepare()
                } catch (e: Exception) {
                    isValid = false
                    errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
                }
            }
        }

        fun getInstance(context: Context, name: String, uri: Uri): SoundViewModel {
            val sound = Sound(name, uri)
            return getInstance(context, sound)
        }
    }
}