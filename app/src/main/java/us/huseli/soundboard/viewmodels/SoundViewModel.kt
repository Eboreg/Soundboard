package us.huseli.soundboard.viewmodels

import android.content.Context
import android.text.Editable
import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SoundViewModel
@Inject constructor(
    private val repository: SoundRepository,
    private val undoRepository: UndoRepository
) : ViewModel() {

    val allSounds = repository.listLiveExtended()

    fun moveFilesToLocalStorage(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        /** One-time thing at app version upgrade */
        val newSounds = mutableListOf<Sound>()
        val oldSounds = repository.list()
        oldSounds.forEach { oldSound ->
            newSounds.add(Sound.createFromTemporary(oldSound, context))
        }
        repository.delete(oldSounds.mapNotNull { it.id })
        repository.insert(newSounds)
    }

    fun saveChecksums() = viewModelScope.launch(Dispatchers.IO) {
        /** One-time thing at app version upgrade */
        repository.list().forEach { sound ->
            try {
                if (sound.checksum == "") repository.updateChecksum(sound.id, sound.calculateChecksum())
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error when saving checksum for $sound: $e")
            }
        }
    }

    fun updateCategoryAndOrder(soundIds: List<Int>, categoryId: Int?) {
        if (categoryId != null)
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateCategoryAndOrder(soundIds, categoryId)
                undoRepository.pushState()
            }
    }


    /******* FILTERING *******/
    private val _filterEnabled = MutableLiveData(false)
    private val _filterTerm = MutableLiveData("")

    private val _filteredSounds = _filterTerm.switchMap { term ->
        allSounds.map { sounds ->
            sounds.filter { sound ->
                sound.name.toLowerCase(Locale.getDefault())
                    .contains(term.toLowerCase(Locale.getDefault()))
            }
        }
    }

    val filterEnabled: LiveData<Boolean>
        get() = _filterEnabled

    val filteredSounds = _filterEnabled.switchMap {
        when (it) {
            true -> _filteredSounds
            else -> allSounds
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
        soundSelectionListeners.clear()
    }


    /******* SOUND SELECTION *******/
    private val soundSelectionListeners = mutableSetOf<SoundSelectionListener>()
    private val _selectEnabled = MutableLiveData(false)
    private val _selectedSounds = mutableSetOf<Sound>()

    val isSelectEnabled: Boolean
        get() = _selectEnabled.value == true
    val selectEnabled: LiveData<Boolean>
        get() = _selectEnabled
    val selectedSounds: List<Sound>
        get() = _selectedSounds.toList()

    private fun deselect(sound: Sound?) {
        if (sound != null) {
            _selectedSounds.remove(sound)
            soundSelectionListeners.filter { it.sound == sound }.forEach { it.onDeselect() }
            if (_selectedSounds.size == 0) disableSelect()
        }
    }

    fun addSoundSelectionListener(listener: SoundSelectionListener) {
        soundSelectionListeners.add(listener)
    }

    fun disableSelect() {
        if (_selectEnabled.value != false) _selectEnabled.value = false
        soundSelectionListeners.forEach { it.onDeselect() }
        _selectedSounds.clear()
    }

    fun enableSelect() {
        if (_selectEnabled.value != true) _selectEnabled.value = true
    }

    fun removeOnSelectAllListener(listener: SoundSelectionListener) {
        soundSelectionListeners.remove(listener)
    }

    fun isSoundSelected(sound: Sound) = _selectedSounds.contains(sound)

    fun select(sound: Sound?) {
        if (sound != null && !_selectedSounds.contains(sound)) {
            _selectedSounds.add(sound)
            soundSelectionListeners.filter { it.sound == sound }.forEach { it.onSelect() }
        }
    }

    fun selectAll() {
        filteredSounds.value?.forEach { select(it) }
    }

    fun selectAllFromSoundToLastSelected(sound: Sound?) {
        // If sound is somehow already selected; skip it
        if (!_selectedSounds.contains(sound) && sound != null) {
            select(sound)
            filteredSounds.value?.let { sounds ->
                _selectedSounds.filterNot { it == sound }.lastOrNull()?.let { lastSelected ->
                    val pos1 = sounds.indexOf(sound)
                    val pos2 = sounds.indexOf(lastSelected)
                    if (pos1 != -1 && pos2 != -1) {
                        val start = if (pos1 < pos2) pos1 else pos2
                        val end = if (start == pos1) pos2 else pos1
                        for (pos in (start + 1) until end) select(sounds[pos])
                    }
                }
            }
        }
    }

    fun toggleSelect(sound: Sound?) {
        if (sound != null) {
            if (_selectedSounds.contains(sound)) deselect(sound)
            else select(sound)
        }
    }


    interface SoundSelectionListener {
        fun onSelect()
        fun onDeselect()
        val sound: Sound?
    }


    companion object {
        const val LOG_TAG = "SoundViewModel"
    }
}