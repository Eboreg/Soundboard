package us.huseli.soundboard_kotlin.data

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundViewModel(application: Application) : AndroidViewModel(application) {
    // Private fields
    private val catRepository: CategoryRepository
    private val repository: SoundRepository

    init {
        val db = SoundDatabase.getInstance(application, viewModelScope)
        repository = SoundRepository(db.soundDao())
        catRepository = CategoryRepository(db.categoryDao())
    }

    lateinit var sound: Sound
    private var mediaPlayer = MediaPlayer()

    // Model fields
    var volume: Int = 100
        set(value) {
            field = value
            // Volume = 0 - 100 here, although MediaPlayer uses 0.0F - 1.0F internally
            mediaPlayer.setVolume(field.toFloat() / 100, field.toFloat() / 100)
        }
    var category: Category? = null

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
        sound?.let {
            it.categoryId = category?.id ?: 0
            it.volume = volume
            when (it.id) {
                null -> insert()
                else -> update()
            }
        }
    }

    fun setOnCompletionListener(function: () -> Unit) {
        mediaPlayer.setOnCompletionListener { function() }
    }

    private fun insert() = viewModelScope.launch(Dispatchers.IO) { sound?.let { repository.insert(it) } }

    private fun update() = viewModelScope.launch(Dispatchers.IO) { sound?.let { repository.update(it) } }

    companion object {
        fun getInstance(application: Application, sound: Sound): SoundViewModel {
            return SoundViewModel(application).apply {
                this.sound = sound
                try {
                    mediaPlayer.setDataSource(application, sound.uri)
                    mediaPlayer.prepare()
                } catch (e: Exception) {
                    isValid = false
                    errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
                }
            }
        }

        fun getInstance(application: Application, name: String, uri: Uri): SoundViewModel {
            val sound = Sound(name, uri)
            return getInstance(application, sound)
        }
    }
}