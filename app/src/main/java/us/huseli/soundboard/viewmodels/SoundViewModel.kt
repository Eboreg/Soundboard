package us.huseli.soundboard.viewmodels

import android.content.Context
import android.text.Editable
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.SoundPlayer
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class SoundViewModel : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application)
    private val repository = SoundRepository(database.soundDao())

    private val _failedSounds = mutableListOf<Sound>()
    private val _filterEnabled = MutableLiveData(false)
    private val _filterTerm = MutableLiveData("")
    private val _reorderEnabled = MutableLiveData(false)
    private val _sounds = repository.list()

    val failedSounds: List<Sound>
        get() = _failedSounds

    val filterEnabled: LiveData<Boolean>
        get() = _filterEnabled

    val sounds = _filterEnabled.switchMap {
        when (it) {
            true -> _filteredSounds
            else -> _sounds
        }
    }

    fun disableFilter() {
        _filterEnabled.value = false
    }

    fun listByCategory(categoryId: Int?) = sounds.map { sound -> sound.filter { it.categoryId == categoryId } }

    fun replaceSound(soundId: Int, sound: Sound, context: Context) {
        _failedSounds.find { it.id == soundId }?.let { oldSound ->
            sound.id = soundId
            viewModelScope.launch(Dispatchers.IO) {
                _failedSounds.remove(oldSound)
                repository.update(sound)
                //addPlayer(sound.uri, SoundPlayer(context, sound.uri, sound.volume))
                //_players[sound.uri] = SoundPlayer(context, sound.uri, sound.volume)
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


    /******* FILTERING *******/
    private val _filteredSounds = _filterTerm.switchMap { term ->
        _sounds.map { sounds ->
            sounds.filter { sound ->
                sound.name.toLowerCase(Locale.getDefault()).contains(term.toLowerCase(Locale.getDefault()))
            }
        }
    }

    fun setFilterTerm(term: Editable?) {
        _filterTerm.value = term?.toString()
    }

    fun toggleFilterEnabled() {
        _filterEnabled.value = _filterEnabled.value != true
    }


    /******* PLAYER RELATED STUFF *******/
    private val _players = HashMap<Sound, SoundPlayer>()

    val players: LiveData<HashMap<Sound, SoundPlayer>> = _sounds.map { sounds ->
        // Add players for new sounds
        sounds.filter { it !in _players.keys }.forEach {
            _players[it] = SoundPlayer(GlobalApplication.application, it.uri, it.volume)
        }
        // Remove players for old sounds
        _players.keys.filter { sound -> sound !in sounds }.forEach { _players.remove(it) }
        _players
    }

    fun setVolume(sound: Sound, volume: Int) {
        _players[sound]?.volume = volume
    }


    /******* RESPOND TO ACTIVITY STATE CHANGES *******/
    override fun onCleared() {
        super.onCleared()
        Log.i(LOG_TAG, "Owner activity finished, releasing and removing SoundPlayers")
        _players.forEach {
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