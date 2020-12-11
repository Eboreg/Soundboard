package us.huseli.soundboard.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.SoundPlayer
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase
import kotlin.collections.set

class SoundViewModel : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application)
    private val repository = SoundRepository(database.soundDao())
    private val players = HashMap<Uri, SoundPlayer>()
    private val _failedSounds = mutableListOf<Sound>()
    private val _reorderEnabled = MutableLiveData(false)

    val failedSounds: List<Sound>
        get() = _failedSounds
    val sounds = repository.list()

    fun listByCategory(categoryId: Int?) = sounds.map { sound -> sound.filter { it.categoryId == categoryId } }

    fun replaceSound(soundId: Int, sound: Sound, context: Context) {
        _failedSounds.find { it.id == soundId }?.let { oldSound ->
            sound.id = soundId
            viewModelScope.launch(Dispatchers.IO) {
                _failedSounds.remove(oldSound)
                repository.update(sound)
                players[sound.uri] = SoundPlayer(context, sound.uri, sound.volume)
            }
        }
    }

    fun update(sounds: List<Sound>, categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        sounds.forEachIndexed { index, sound ->
            sound.order = index
            sound.categoryId = categoryId
        }
        repository.update(sounds)
    }

    /******* SOUNDPLAYER STUFF *******/
    private fun getPlayer(sound: Sound): SoundPlayer? {
        return players[sound.uri]?.apply {
            // Conveniently update volume in case it's been changed
            if (volume != sound.volume) volume = sound.volume
        }
    }

    fun getPlayer(sound: Sound, context: Context): SoundPlayer {
        return getPlayer(sound) ?: initPlayer(sound, context)
    }

    private fun initPlayer(sound: Sound, context: Context): SoundPlayer {
        return players[sound.uri] ?: SoundPlayer(context, sound.uri, sound.volume).also {
            viewModelScope.launch(Dispatchers.IO) { players[sound.uri] = it }
            if (it.noPermission) _failedSounds.add(sound)
        }
    }

    /**
     * Only to be used when we have fetched ALL sounds
     */
    fun initPlayers(sounds: List<Sound>, context: Context) = viewModelScope.launch(Dispatchers.IO) {
        // Remove players for no longer existing sounds
        players.filterNot { entry -> entry.key in sounds.map { it.uri } }.forEach {
            it.value.release()
            players.remove(it.key)
        }
        sounds.filterNot { sound -> sound.uri in players.map { it.key } }.forEach { initPlayer(it, context) }
    }

    /******* RESPOND TO ACTIVITY STATE CHANGES *******/
    override fun onCleared() {
        super.onCleared()
        Log.i(LOG_TAG, "Owner activity finished, releasing and removing SoundPlayers")
        players.forEach {
            it.value.release()
        }
        onSelectAllListeners.clear()
    }


    /******* SOUND SELECTION *******/
    private val onSelectAllListeners = mutableListOf<OnSelectAllListener>()
    private val _selectEnabled = MutableLiveData(false)
    private val _selectedSounds = mutableSetOf<Sound>()

    val selectEnabled: LiveData<Boolean>
        get() = _selectEnabled
    val selectedSounds: List<Sound>
        get() = _selectedSounds.toList()

    fun select(sound: Sound) = _selectedSounds.add(sound)

    fun deselect(sound: Sound) {
        _selectedSounds.remove(sound)
        if (_selectedSounds.size == 0) disableSelect()
    }

    fun getLastSelected(categoryId: Int, except: Sound): Sound? {
        return try {
            _selectedSounds.last { it.categoryId == categoryId && it != except }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun enableSelect() {
        if (_selectEnabled.value != true) _selectEnabled.value = true
        if (_reorderEnabled.value != false) _reorderEnabled.value = false
    }

    fun disableSelect() {
        if (_selectEnabled.value != false) _selectEnabled.value = false
        _selectedSounds.clear()
    }

    fun selectAll() {
        onSelectAllListeners.forEach { it.select() }
    }

    fun addOnSelectAllListener(listener: OnSelectAllListener) {
        onSelectAllListeners.add(listener)
    }

    fun removeOnSelectAllListener(listener: OnSelectAllListener) {
        onSelectAllListeners.remove(listener)
    }

    fun isSelected(sound: Sound) = _selectedSounds.contains(sound)


    /******* SOUND REORDERING *******/
    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }


    interface OnSelectAllListener {
        fun select()
    }


    companion object {
        const val LOG_TAG = "SoundViewModel"
    }
}