package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

class SoundViewModel(val sound: Sound) : ViewModel(), OrderableItem {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())
    private val colorHelper = ColorHelper(GlobalApplication.application)
    // This seems kinda roundabout, but I guess it works?
    private val _sound = repository.get(sound.id)
    private val player = GlobalApplication.application.getPlayer(sound).apply {
        setOnCompletionListener { this@SoundViewModel.pause() }
    }
    private val _isPlaying = MutableLiveData(player.isPlaying)
    private val _isSelected = MutableLiveData(false)

    val errorMessage = player.errorMessage
    val isValid = player.isValid

    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    val isSelected: LiveData<Boolean>
        get() = _isSelected

    val backgroundColor = _sound.switchMap { repository.getBackgroundColor(it?.categoryId) }
    val textColor = backgroundColor.map { colorHelper.getTextColorForBackgroundColor(it) }

    /** Model fields */
    val id = sound.id
    val categoryId = _sound.map { it?.categoryId }
    val name = _sound.map { it?.name ?: "" }
    val volume = _sound.map { it?.volume ?: 100 }
    override var order = sound.order

    override fun toString() = sound.name

    fun toggleSelected() {
        _isSelected.value = !_isSelected.value!!
    }

    fun select() {
        _isSelected.value = true
    }

    fun unselect() {
        _isSelected.value = false
    }

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