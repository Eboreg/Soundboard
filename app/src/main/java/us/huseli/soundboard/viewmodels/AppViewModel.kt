package us.huseli.soundboard.viewmodels

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

@HiltViewModel
class AppViewModel @Inject constructor(
    private val soundRepository: SoundRepository,
    private val undoRepository: UndoRepository
) : ViewModel() {

    private val _orientation = MutableLiveData<Int>()
    private val _reorderEnabled = MutableLiveData(false)
    private val _repressMode = MutableLiveData(SoundPlayer.RepressMode.STOP)
    private val _screenRatio = MutableLiveData<Double>()  // (width / height) in portrait mode
    private val _spanCountLandscape = MutableLiveData<Int?>()
    private val _spanCountPortrait = MutableLiveData<Int?>()

    val repressMode: LiveData<SoundPlayer.RepressMode>
        get() = _repressMode

    val spanCountLandscape: LiveData<Int?>
        get() = _spanCountLandscape

    val spanCount = _orientation.switchMap {
        when (it) {
            Configuration.ORIENTATION_PORTRAIT -> _spanCountPortrait
            else -> _spanCountLandscape
        }
    }

    val isRedoPossible: LiveData<Boolean>
        get() = undoRepository.isRedoPossible

    val isUndoPossible: LiveData<Boolean>
        get() = undoRepository.isUndoPossible

    val zoomInPossible = spanCount.map { it != null && it > 1 }

    fun zoomIn() = zoom(-1)

    fun zoomOut() = zoom(1)

    fun setRepressMode(value: SoundPlayer.RepressMode) {
        _repressMode.postValue(value)
    }

    fun deleteOrphans(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val sounds = soundRepository.list()
        context.getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE)?.listFiles()?.forEach { file ->
            if (!sounds.map { it.path }.contains(file.path))
                file.delete()
        }
    }

    fun redo() = viewModelScope.launch(Dispatchers.IO) { undoRepository.redo() }

    fun undo() = viewModelScope.launch(Dispatchers.IO) { undoRepository.undo() }

    /******* SOUND/CATEGORY REORDERING *******/
    val isReorderEnabled: Boolean
        get() = _reorderEnabled.value == true
    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }

    fun disableReorder() {
        if (_reorderEnabled.value != false) _reorderEnabled.value = false
    }


    /** PRIVATE METHODS */
    private fun getZoomPercent(): Int? {
        return when (_orientation.value) {
            Configuration.ORIENTATION_LANDSCAPE ->
                _spanCountLandscape.value?.let { ((Constants.DEFAULT_SPANCOUNT_LANDSCAPE.toDouble() / it) * 100).roundToInt() }
            Configuration.ORIENTATION_PORTRAIT -> {
                landscapeSpanCountToPortrait(Constants.DEFAULT_SPANCOUNT_LANDSCAPE)?.let { defaultSpanCount ->
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

    fun setupLayout(orientation: Int, screenWidthDp: Int, screenHeightDp: Int, landscapeSpanCount: Int): Int {
        /**
         * Returns actual span count for the current screen orientation
         */
        val localLandscapeSpanCount =
            if (landscapeSpanCount > 0) landscapeSpanCount else Constants.DEFAULT_SPANCOUNT_LANDSCAPE
        _orientation.value = orientation

        val ratio = when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> screenHeightDp.toDouble() / screenWidthDp
            else -> screenWidthDp.toDouble() / screenHeightDp
        }
        _screenRatio.value = ratio

        _spanCountLandscape.value = localLandscapeSpanCount
        val localPortraitSpanCount = landscapeSpanCountToPortrait(localLandscapeSpanCount, ratio)
        _spanCountPortrait.value = localPortraitSpanCount

        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) localLandscapeSpanCount else localPortraitSpanCount
    }

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