package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.interfaces.OrderableItem

class SoundViewModel(private val _sound: Sound) : ViewModel(), OrderableItem {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application, viewModelScope).soundDao())
    private val colorHelper = ColorHelper(GlobalApplication.application)
    private val player = GlobalApplication.application.getPlayer(_sound).apply {
        setOnCompletionListener { this@SoundViewModel.pause() }
    }
    private val _isPlaying = MutableLiveData(player.isPlaying)
    private val _isSelected = MutableLiveData(false)

    /**
     * Reasoning behind having a LiveData Sound _and_ a Sound as an initializer parameter:
     * We want an observable Sound object, that gets updated as the backend data updates.
     * We also want to, without any unnecessary delays or hassle, be able to init a SoundPlayer
     * and set those parameters that don't change once a Sound is saved (id, uri)
     */
    val sound = repository.get(_sound.id)

    val errorMessage = player.errorMessage
    val isValid = player.isValid

    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    val isSelected: LiveData<Boolean>
        get() = _isSelected

    val backgroundColor = sound.switchMap { repository.getBackgroundColor(it?.categoryId) }
    val textColor = backgroundColor.map { colorHelper.getTextColorForBackgroundColor(it) }

    /** Model fields */
    val id = _sound.id
    val categoryId = sound.map { it?.categoryId }
    val name = sound.map { it?.name ?: "" }
    val volume = sound.map { it?.volume ?: 100 }
    override var order = _sound.order

    override fun toString() = _sound.name

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