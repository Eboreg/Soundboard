package us.huseli.soundboard.viewmodels

import android.content.res.Configuration
import androidx.lifecycle.*
import us.huseli.soundboard.SoundPlayer
import kotlin.math.max
import kotlin.math.roundToInt

class AppViewModel : ViewModel() {
    companion object {
        const val DEFAULT_SPANCOUNT_LANDSCAPE = 8
        const val DEFAULT_SPANCOUNT_PORTRAIT = 4
    }

    private val _orientation = MutableLiveData<Int>()
    private val _repressMode = MutableLiveData(SoundPlayer.RepressMode.STOP)
    private val _screenRatio = MutableLiveData<Double>()  // (width / height) in portrait mode
    private val _spanCountLandscape = MutableLiveData<Int>()
    private val _spanCountPortrait = MutableLiveData<Int>()

    val repressMode: LiveData<SoundPlayer.RepressMode>
        get() = _repressMode

    val spanCountLandscape: LiveData<Int>
        get() = _spanCountLandscape

    val spanCount = _orientation.switchMap {
        when (it) {
            Configuration.ORIENTATION_PORTRAIT -> _spanCountPortrait
            else -> _spanCountLandscape
        }
    }

    val zoomInPossible = spanCount.map { it != null && it > 1 }

    fun zoomIn() = zoom(-1)

    fun zoomOut() = zoom(1)

    fun cycleRepressMode() {
        _repressMode.value = _repressMode.value?.let {
            when (it) {
                SoundPlayer.RepressMode.STOP -> SoundPlayer.RepressMode.RESTART
                SoundPlayer.RepressMode.RESTART -> SoundPlayer.RepressMode.OVERLAP
                SoundPlayer.RepressMode.OVERLAP -> SoundPlayer.RepressMode.STOP
            }
        } ?: SoundPlayer.RepressMode.STOP
    }


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