package us.huseli.soundboard_kotlin.viewmodels

import android.content.res.Configuration
import androidx.lifecycle.*
import us.huseli.soundboard_kotlin.data.Sound
import kotlin.math.max
import kotlin.math.roundToInt

class AppViewModel : ViewModel() {
    companion object {
        const val DEFAULT_SPANCOUNT_LANDSCAPE = 8
    }

    private val _orientation = MutableLiveData<Int>()
    private val _screenRatio = MutableLiveData<Double>()  // (width / height) in portrait mode
    private val _spanCountLandscape = MutableLiveData<Int>()
    private val _spanCountPortrait = MutableLiveData<Int>()

    private val _reorderEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    private val _selectEnabled = MutableLiveData(false)
    private val _selectedSounds = mutableListOf<Sound>()

    val selectEnabled: LiveData<Boolean>
        get() = _selectEnabled

    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

    val spanCountLandscape: LiveData<Int>
        get() = _spanCountLandscape

    val spanCount = _orientation.switchMap {
        when (it) {
            Configuration.ORIENTATION_PORTRAIT -> _spanCountPortrait
            else -> _spanCountLandscape
        }
    }

    val zoomInPossible = spanCount.map { it != null && it > 1 }

    fun getSelectedSounds() = _selectedSounds.toList()

    fun selectSound(sound: Sound) {
        enableSelect()
        if (!_selectedSounds.contains(sound)) _selectedSounds.add(sound)
    }

    fun deselectSound(sound: Sound) {
        _selectedSounds.remove(sound)
        if (_selectedSounds.size == 0)
            disableSelect()
    }

    private fun enableSelect() {
        if (_selectEnabled.value != true) _selectEnabled.value = true
        if (_reorderEnabled.value != false) _reorderEnabled.value = false
    }

    fun disableSelect() {
        if (_selectEnabled.value != false) _selectEnabled.value = false
        _selectedSounds.clear()
    }

    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }

    fun zoomIn() = zoom(-1)

    fun zoomOut() = zoom(1)


    /** PRIVATE METHODS */

    @Suppress("LocalVariableName")
    fun setup(orientation: Int, screenWidthDp: Int, screenHeightDp: Int, landscapeSpanCount: Int): Int {
        /**
         * Returns actual span count for the current screen orientation
         */
        val _landscapeSpanCount = if (landscapeSpanCount > 0) landscapeSpanCount else DEFAULT_SPANCOUNT_LANDSCAPE
        _orientation.value = orientation

        val ratio = when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> screenHeightDp.toDouble() / screenWidthDp
            else -> screenWidthDp.toDouble() / screenHeightDp
        }
        _screenRatio.value = ratio

        _spanCountLandscape.value = _landscapeSpanCount
        val _portraitSpanCount = landscapeSpanCountToPortrait(_landscapeSpanCount, ratio)
        _spanCountPortrait.value = _portraitSpanCount

        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) _landscapeSpanCount else _portraitSpanCount
    }

    private fun getZoomPercent(): Int? {
        return when (_orientation.value) {
            Configuration.ORIENTATION_LANDSCAPE ->
                _spanCountLandscape.value?.let { ((DEFAULT_SPANCOUNT_LANDSCAPE.toDouble() / it) * 100).roundToInt() }
            Configuration.ORIENTATION_PORTRAIT -> {
                landscapeSpanCountToPortrait(DEFAULT_SPANCOUNT_LANDSCAPE)?.let { defaultSpanCount ->
                    _spanCountPortrait.value?.let { ((defaultSpanCount.toDouble() / it) * 100).roundToInt() }
                }
            }
            else -> null
        }
    }

    private fun landscapeSpanCountToPortrait(spanCount: Int, ratio: Double) = max((spanCount * ratio).roundToInt(), 1)

    private fun landscapeSpanCountToPortrait(spanCount: Int): Int? =
            _screenRatio.value?.let { ratio -> landscapeSpanCountToPortrait(spanCount, ratio) }

    private fun portraitSpanCountToLandscape(spanCount: Int): Int? =
            _screenRatio.value?.let { ratio -> (spanCount / ratio).roundToInt() }

    private fun zoom(factor: Int): Int? {
        // factor -1 = spanCount += -1 = zoom in
        when (_orientation.value) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                _spanCountLandscape.value?.let { spanCount ->
                    if (spanCount + factor >= 1) {
                        _spanCountLandscape.value = spanCount + factor
                        _spanCountPortrait.value = landscapeSpanCountToPortrait(spanCount + factor)
                    }
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                _spanCountPortrait.value?.let { spanCount ->
                    if (spanCount + factor >= 1) {
                        _spanCountPortrait.value = spanCount + factor
                        _spanCountLandscape.value = portraitSpanCountToLandscape(spanCount + factor)
                    }
                }
            }
        }
        return getZoomPercent()
    }
}