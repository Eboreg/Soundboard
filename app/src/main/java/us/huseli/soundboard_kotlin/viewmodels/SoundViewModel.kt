package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

//class SoundViewModel(soundId: Int) : ViewModel() {
class SoundViewModel(val sound: Sound) : ViewModel(), OrderableItem {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())
    private val colorHelper = ColorHelper(GlobalApplication.application)
    private val player = GlobalApplication.application.getPlayer(sound).apply {
        setOnCompletionListener { this@SoundViewModel.pause() }
    }
    private val _isPlaying = MutableLiveData(player.isPlaying)
    private val _categoryId = repository.getCategoryId(sound)

    override fun toString() = sound.name

    val errorMessage = player.errorMessage
    val isValid = player.isValid

    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    /** Model fields */
    val id = sound.id
    val categoryId = repository.getCategoryId(sound)
    val name = sound.name
    val uri = sound.uri
    override var order = sound.order
    val backgroundColor = _categoryId.switchMap { repository.getBackgroundColor(it) }
    //val backgroundColor = categoryId?.let { repository.getBackgroundColor(it) } ?: liveData { Color.DKGRAY }
    val textColor = backgroundColor.map { colorHelper.getTextColorForBackgroundColor(it) }
    val volume = sound.volume

    fun playOrPause() = if (player.isPlaying) pause() else play()

    private fun pause() {
        player.pause()
        _isPlaying.value = false
    }
    private fun play() {
        player.play()
        _isPlaying.value = true
    }
}