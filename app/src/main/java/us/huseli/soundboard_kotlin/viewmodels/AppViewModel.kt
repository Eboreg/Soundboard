package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    private val _zoomLevel = MutableLiveData(0)
    private val _reorderEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _zoomInPossible = MutableLiveData(true)

    val zoomInPossible: LiveData<Boolean>
        get() = _zoomInPossible

    val zoomLevel: LiveData<Int>
        get() = _zoomLevel

    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

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