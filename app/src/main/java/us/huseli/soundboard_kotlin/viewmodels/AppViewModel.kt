package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    private val _zoomLevel = MutableLiveData(0)
    val zoomLevel: LiveData<Int>
        get() = _zoomLevel
    fun zoomIn() {
        _zoomLevel.value = (_zoomLevel.value ?: 0) + 1
    }
    fun zoomOut() {
        _zoomLevel.value = (_zoomLevel.value ?: 0) - 1
    }
    fun setZoomLevel(value: Int) {
        _zoomLevel.value = value
    }

    private val _reorderEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled
    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }
}