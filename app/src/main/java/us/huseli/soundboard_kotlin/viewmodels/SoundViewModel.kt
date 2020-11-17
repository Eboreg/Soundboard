package us.huseli.soundboard_kotlin.viewmodels

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundRepository
import us.huseli.soundboard_kotlin.data.SoundboardDatabase

class SoundViewModel(override val sound: Sound) : AbstractSoundViewModel() {
    private val repository = SoundRepository(SoundboardDatabase.getInstance(GlobalApplication.application).soundDao())
    private val player = GlobalApplication.application.getPlayer(sound.uri).apply {
        setOnCompletionListener { this@SoundViewModel.pause() }
    }

    init {
        Log.d(LOG_TAG, "init: sound=$sound, player=$player")
        viewModelScope.launch(Dispatchers.IO) {
            player.setup()
            if (!player.isValid) {
                _isValid.postValue(false)
                errorMessage = player.errorMessage
            }
            _duration.postValue("${player.duration}s")
        }
    }

    private val _isPlaying = MutableLiveData(false)
    private val _isSelected = MutableLiveData(false)
    private val _isValid = MutableLiveData(true)
    private val _duration = MutableLiveData<String>()

    /**
     * Reasoning behind having a LiveData Sound _and_ a Sound as an initializer parameter:
     * We want an observable Sound object, that gets updated as the backend data updates.
     * We also want to, without any unnecessary delays or hassle, be able to init a SoundPlayer
     * and set those parameters that don't change once a Sound is saved (id, uri)
     */
    //private val soundLiveData = repository.getLiveData(sound.id)

    override var errorMessage = ""

    val isValid: LiveData<Boolean>
        get() = _isValid

    override val duration: LiveData<String>
        get() = _duration

    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    val isSelected: LiveData<Boolean>
        get() = _isSelected

    override val backgroundColor = repository.getBackgroundColor(sound.categoryId)
    //override val backgroundColor: LiveData<Int> = soundLiveData.switchMap { repository.getBackgroundColor(it?.categoryId) }

    override val textColor = backgroundColor.map { GlobalApplication.colorHelper.getTextColorForBackgroundColor(it) }


    /** Model fields */
    val id = sound.id
    //val volume = sound.map { it?.volume ?: 100 }
    var order: Int = sound.order
    override val name = liveData { emit(sound.name) }
    // override val name = soundLiveData.map { it?.name ?: "" }
    override val volume = liveData { emit(sound.volume) }
    // override val volume = soundLiveData.map { it?.volume ?: 100 }

    /** Public methods */
    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundViewModel $hashCode <sound=$sound>"
    }

    fun toggleSelected() {
        _isSelected.value = !(_isSelected.value ?: false)
    }

    fun select() {
        _isSelected.value = true
    }

    fun unselect() {
        _isSelected.value = false
    }

    fun playOrPause() = if (player.isPlaying) pause() else play()


    /** Private methods */
    private fun pause() {
        player.pause()
        _isPlaying.value = false
    }

    private fun play() {
        player.play()
        _isPlaying.value = true
    }


    companion object {
        const val LOG_TAG = "SoundViewModel"
    }
}
