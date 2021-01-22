package us.huseli.soundboard.viewmodels

import android.content.Context
import android.text.Editable
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.helpers.MD5
import java.io.File
import java.util.*

class SoundViewModel @ViewModelInject constructor(private val repository: SoundRepository) : ViewModel() {
    private val _failedSounds = mutableListOf<Sound>()
    private val _filterEnabled = MutableLiveData(false)
    private val _filterTerm = MutableLiveData("")
    private val _reorderEnabled = MutableLiveData(false)
    private val _sounds = repository.listLive()

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

    @Suppress("UNUSED_PARAMETER")
    // TODO: Not used ATM
    fun replaceSound(soundId: Int, sound: Sound, context: Context) {
        _failedSounds.find { it.id == soundId }?.let { oldSound ->
            sound.id = soundId
            viewModelScope.launch(Dispatchers.IO) {
                _failedSounds.remove(oldSound)
                repository.update(sound)
            }
        }
    }

    fun moveFilesToLocalStorage(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        /** One-time thing at app version upgrade */
        val newSounds = mutableListOf<Sound>()
        val oldSounds = repository.list()
        oldSounds.forEach { oldSound ->
            newSounds.add(Sound.createFromTemporary(oldSound, context))
        }
        repository.delete(oldSounds)
        repository.insert(newSounds)
    }

    fun saveChecksums() = viewModelScope.launch(Dispatchers.IO) {
        /** One-time thing at app version upgrade */
        val updatedSounds = mutableListOf<Sound>()
        repository.list().forEach { sound ->
            sound.uri.path?.let { path ->
                try {
                    val file = File(path)
                    if (sound.checksum == null) {
                        sound.checksum = MD5.calculate(file)
                        updatedSounds.add(sound)
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error when saving checksum for $sound: $e")
                }
            }
        }
        repository.update(updatedSounds)
    }

    fun sort(categoryId: Int, sortBy: Sound.SortParameter, sortOrder: Sound.SortOrder) = viewModelScope.launch(Dispatchers.IO) {
        val sounds = repository.listByCategory(categoryId).toMutableList().sortedWith(Sound.Comparator(sortBy, sortOrder))
        update(sounds, categoryId)
    }

    fun update(sounds: List<Sound>, categoryId: Int) = viewModelScope.launch(Dispatchers.IO) {
        //GlobalApplication.pushUndoState(_sounds.value, null)
        sounds.forEachIndexed { index, sound ->
            sound.order = index
            sound.categoryId = categoryId
        }
        repository.update(sounds)
    }

    fun update(sounds: List<Sound>, category: Category?) = category?.id?.let { update(sounds, it) }

    fun updateDuration(sound: Sound, duration: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateDuration(sound, duration)
    }


    /******* FILTERING *******/
    private val _filteredSounds = _filterTerm.switchMap { term ->
        _sounds.map { sounds ->
            sounds.filter { sound ->
                sound.name.toLowerCase(Locale.getDefault()).contains(term.toLowerCase(Locale.getDefault()))
            }
        }
    }

    fun disableFilter() {
        _filterEnabled.value = false
    }

    fun enableFilter() {
        _filterEnabled.value = true
    }

    fun setFilterTerm(term: Editable?) {
        _filterTerm.value = term?.toString()
    }

    fun toggleFilterEnabled() {
        _filterEnabled.value = _filterEnabled.value != true
    }


    /******* RESPOND TO ACTIVITY STATE CHANGES *******/
    override fun onCleared() {
        super.onCleared()
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

    fun addOnSelectAllListener(listener: OnSelectAllListener) = onSelectAllListeners.add(listener)

    fun deselect(sound: Sound) {
        _selectedSounds.remove(sound)
        if (_selectedSounds.size == 0) disableSelect()
    }

    fun disableSelect() {
        if (_selectEnabled.value != false) _selectEnabled.value = false
        _selectedSounds.clear()
    }

    fun enableSelect() {
        if (_selectEnabled.value != true) _selectEnabled.value = true
        if (_reorderEnabled.value != false) _reorderEnabled.value = false
    }

    fun getLastSelected(category: Category?, except: Sound) = category?.id?.let { categoryId ->
        try {
            _selectedSounds.last { it.categoryId == categoryId && it != except }
        } catch (e: NoSuchElementException) {
            null
        }
    }

    fun isSelected(sound: Sound) = _selectedSounds.contains(sound)

    fun removeOnSelectAllListener(listener: OnSelectAllListener) {
        onSelectAllListeners.remove(listener)
    }

    fun select(sound: Sound) = _selectedSounds.add(sound)

    fun selectAll() {
        onSelectAllListeners.forEach { it.select() }
    }


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
        const val MAX_UNDO_STATES = 20
    }
}