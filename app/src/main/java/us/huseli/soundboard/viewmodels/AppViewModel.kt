package us.huseli.soundboard.viewmodels

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.UndoRepository
import us.huseli.soundboard.helpers.SettingsManager
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsManager: SettingsManager,
    private val soundRepository: SoundRepository,
    private val undoRepository: UndoRepository
) : ViewModel() {

    private val _repressMode = MutableLiveData(settingsManager.getRepressMode())

    val repressMode: LiveData<SoundPlayer.RepressMode>
        get() = _repressMode

    fun setRepressMode(value: SoundPlayer.RepressMode) {
        _repressMode.postValue(value)
    }

    fun deleteOrphans(soundDir: File?) = viewModelScope.launch(Dispatchers.IO) {
        val paths = soundRepository.listPaths()
        soundDir?.listFiles()?.forEach { file -> if (!paths.contains(file.path)) file.delete() }
    }

    /********* UNDO/REDO *********************************************************************************************/
    val isRedoPossible: LiveData<Boolean>
        get() = undoRepository.isRedoPossible

    val isUndoPossible: LiveData<Boolean>
        get() = undoRepository.isUndoPossible

    fun redo() = viewModelScope.launch(Dispatchers.IO) { undoRepository.redo() }

    fun undo() = viewModelScope.launch(Dispatchers.IO) { undoRepository.undo() }


    /********* SOUND/CATEGORY REORDERING *****************************************************************************/
    private val _reorderEnabled = MutableLiveData(false)

    val reorderEnabled: LiveData<Boolean>
        get() = _reorderEnabled

    fun toggleReorderEnabled() {
        _reorderEnabled.value = !(_reorderEnabled.value ?: false)
    }

    fun disableReorder() {
        if (_reorderEnabled.value != false) _reorderEnabled.value = false
    }


    /********* ZOOM **************************************************************************************************/
    private val orientation = MutableLiveData<Int>()
    private var screenRatio: Double = run {
        val width = context.resources.configuration.screenWidthDp.toDouble()
        val height = context.resources.configuration.screenHeightDp.toDouble()
        min(height, width) / max(height, width)
    }

    private val spanCountLandscape = MutableLiveData(settingsManager.getLandscapeSpanCount())
    private val spanCountPortrait =
        MutableLiveData(landscapeSpanCountToPortrait(settingsManager.getLandscapeSpanCount(), screenRatio))

    val spanCount = orientation.switchMap {
        when (it) {
            Configuration.ORIENTATION_PORTRAIT -> spanCountPortrait
            else -> spanCountLandscape
        }
    }

    val zoomInPossible = spanCount.map { it != null && it > 1 }

    fun zoomIn() = zoom(-1)

    fun zoomOut() = zoom(1)

    private fun getZoomPercent(): Int? {
        return when (orientation.value) {
            Configuration.ORIENTATION_LANDSCAPE -> spanCountLandscape.value?.let {
                ((Constants.DEFAULT_SPANCOUNT_LANDSCAPE.toDouble() / it) * 100).roundToInt()
            }
            Configuration.ORIENTATION_PORTRAIT -> spanCountPortrait.value?.let {
                ((landscapeSpanCountToPortrait(Constants.DEFAULT_SPANCOUNT_LANDSCAPE,
                    screenRatio).toDouble() / it) * 100).roundToInt()
            }
            else -> null
        }
    }

    private fun landscapeSpanCountToPortrait(spanCount: Int, ratio: Double) = max((spanCount * ratio).roundToInt(), 1)

    private fun portraitSpanCountToLandscape(spanCount: Int, ratio: Double) = (spanCount / ratio).roundToInt()

    fun setOrientation(value: Int) {
        orientation.value = value
    }

    private fun zoom(factor: Int): Int? {
        // factor -1 = spanCount += -1 = zoom in
        when (orientation.value) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                spanCountLandscape.value?.let { spanCount ->
                    if (spanCount + factor >= 1) {
                        spanCountLandscape.value = spanCount + factor
                        spanCountPortrait.value = landscapeSpanCountToPortrait(spanCount + factor, screenRatio)
                    }
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                spanCountPortrait.value?.let { spanCount ->
                    if (spanCount + factor >= 1) {
                        spanCountPortrait.value = spanCount + factor
                        spanCountLandscape.value = portraitSpanCountToLandscape(spanCount + factor, screenRatio)
                    }
                }
            }
        }
        spanCountLandscape.value?.also { settingsManager.setLandscapeSpanCount(it) }
        return getZoomPercent()
    }
}