package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.data.Sound

class AppViewModel : ViewModel() {
    private val _zoomLevel = MutableLiveData(0)
    private val _reorderEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _zoomInPossible = MutableLiveData(true)
    private val _selectEnabled = MutableLiveData(false)
    private val _selectedSounds = mutableListOf<Sound>()

    val selectEnabled: LiveData<Boolean>
        get() = _selectEnabled

    val zoomInPossible: LiveData<Boolean>
        get() = _zoomInPossible

    val zoomLevel: LiveData<Int>
        get() = _zoomLevel

    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

    fun getSelectedSounds() = _selectedSounds.toList()

    fun selectSound(sound: Sound) {
        if (_selectEnabled.value != true) _selectEnabled.value = true
        if (!_selectedSounds.contains(sound)) _selectedSounds.add(sound)
    }

    fun deselectSound(sound: Sound) {
        _selectedSounds.remove(sound)
        if (_selectedSounds.size == 0)
            disableSelect()
    }

    fun disableSelect() {
        if (_selectEnabled.value != false) _selectEnabled.value = false
        _selectedSounds.clear()
    }

    fun zoomIn() {
        if (_zoomInPossible.value != false)
            _zoomLevel.value = (_zoomLevel.value ?: 0) + 1
    }

    fun zoomOut() {
        _zoomLevel.value = (_zoomLevel.value ?: 0) - 1
    }

    fun setZoomLevel(value: Int) {
        _zoomLevel.value = value
    }

    fun setZoomInPossible(value: Boolean) {
        _zoomInPossible.value = value
    }

    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }
}