package us.huseli.soundboard.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun getByCategory(categoryId: Int?) = repository.getByCategory(categoryId)

    fun addFailedSound(sound: Sound) = _failedSounds.add(sound)

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
        // Conveniently update volume in case it's been changed
        return players[sound.uri]?.apply { setVolume(sound.volume) }
    }

    fun getPlayer(sound: Sound, context: Context): SoundPlayer {
        return getPlayer(sound) ?: SoundPlayer(context, sound.uri, sound.volume).also {
            players[sound.uri] = it
//            viewModelScope.launch(Dispatchers.IO) {
//                it.setup()
//            }
        }
    }

    /******* RESPOND TO ACTIVITY STATE CHANGES *******/
    override fun onCleared() {
        super.onCleared()
        Log.d(LOG_TAG, "Owner activity finished, releasing and removing SoundPlayers")
        players.forEach {
            it.value.release()
            players.remove(it.key)
        }
        onSelectAllListeners.clear()
    }


    /******* SOUND SELECTION *******/
    private val onSelectAllListeners = mutableListOf<OnSelectAllListener>()
    private val _selectEnabled = MutableLiveData(false)
    private val _selectedSounds = mutableListOf<Sound>()

    val selectEnabled: LiveData<Boolean>
        get() = _selectEnabled
    val selectedSounds: List<Sound>
        get() = _selectedSounds.toList()

    fun select(sound: Sound) {
        if (!_selectedSounds.contains(sound)) _selectedSounds.add(sound)
    }

    fun toggleSelected(sound: Sound): Boolean {
        /**
         * Returns true if sound was selected, false if deselected!
         */
        return when (_selectedSounds.contains(sound)) {
            true -> {
                deselect(sound)
                false
            }
            else -> {
                select(sound)
                true
            }
        }
    }

    private fun deselect(sound: Sound) {
        _selectedSounds.remove(sound)
        if (_selectedSounds.size == 0) disableSelect()
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
        onSelectAllListeners.forEach { it.selectAllSounds() }
    }

    fun addOnSelectAllListener(listener: OnSelectAllListener) {
        onSelectAllListeners.add(listener)
    }

    fun removeOnSelectAllListener(listener: OnSelectAllListener) {
        onSelectAllListeners.remove(listener)
    }


    /******* SOUND REORDERING *******/
    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }


    interface OnSelectAllListener {
        fun selectAllSounds()
    }


    companion object {
        const val LOG_TAG = "SoundViewModel2"
    }
}