package us.huseli.soundboard.viewmodels

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.data.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

@HiltViewModel
class AppViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository, private val soundRepository: SoundRepository
) : ViewModel() {
    private val undoStates = mutableListOf<UndoState>()
    private val _orientation = MutableLiveData<Int>()
    private val _reorderEnabled = MutableLiveData(false)
    private val _repressMode = MutableLiveData(SoundPlayer.RepressMode.STOP)
    private val _screenRatio = MutableLiveData<Double>()  // (width / height) in portrait mode
    private val _spanCountLandscape = MutableLiveData<Int?>()
    private val _spanCountPortrait = MutableLiveData<Int?>()
    private val _undosAvailable = MutableLiveData(false)

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

    val undosAvailable: LiveData<Boolean>
        get() = _undosAvailable

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

    fun deleteOrphans(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val sounds = soundRepository.list()
        context.getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE)?.listFiles()?.forEach { file ->
            if (!sounds.map { it.path }.contains(file.path))
                file.delete()
        }
    }

    fun pushCategoryUndoState(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        pushUndoState(null, categoryRepository.list(), context)
    }

    fun pushSoundUndoState(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        pushUndoState(soundRepository.list(), null, context)
    }

    fun pushSoundAndCategoryUndoState(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        pushUndoState(soundRepository.list(), categoryRepository.list(), context)
    }

    fun undo() = viewModelScope.launch(Dispatchers.IO) {
        undoStates.removeLastOrNull()?.also {
            if (it.categories != null) categoryRepository.reset(it.categories)
            if (it.sounds != null) soundRepository.reset(it.sounds)
        }
        if (undoStates.isEmpty()) _undosAvailable.postValue(false)
    }

    /******* SOUND/CATEGORY REORDERING *******/
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

    private fun pushUndoState(sounds: List<Sound>?, categories: List<Category>?, context: Context) {
        if (sounds != null || categories != null) {
            undoStates.add(UndoState(sounds, categories))
            _undosAvailable.postValue(true)
            if (undoStates.size > Constants.MAX_UNDO_STATES) {
                /** If max undo states is reached, delete the first one */
                val removedState = undoStates.removeFirst()
                val nextState = undoStates.first()
                if (removedState.sounds != null && nextState.sounds != null)
                /** If the deleted state involved removing sounds, now is the time to actually delete the files */
                    removedState.sounds.subtract(nextState.sounds).forEach {
                        context.getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE)?.listFiles()?.forEach { file ->
                            if (file.path == it.path) file.delete()
                        }
                    }
            }
        }
    }

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


    class UndoState(val sounds: List<Sound>?, val categories: List<Category>?)
}