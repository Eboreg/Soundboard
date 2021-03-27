package us.huseli.soundboard.viewmodels

import android.media.audiofx.AudioEffect
import android.media.audiofx.PresetReverb
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.soundboard.data.PlayerRepository
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class AudioEffectViewModel @Inject constructor(private val playerRepository: PlayerRepository) : ViewModel() {
    private val _effectEnabled = MutableLiveData(false)
    private var _linearInputLevel: Int = 72
    private var effect: AudioEffect? = null

    val effectAvailable = AudioEffect.queryEffects().map { it.type }.contains(AudioEffect.EFFECT_TYPE_PRESET_REVERB)
    val effectEnabled: LiveData<Boolean>
        get() = _effectEnabled
    val linearInputLevel: Int
        get() = _linearInputLevel
    var reverbPreset: Short? = null

    private fun linearInputToLevel(value: Int): Float {
        // https://developer.android.com/reference/kotlin/android/media/AudioTrack#setauxeffectsendlevel
        return when {
            value == 0 -> 0f
            value > 72 -> 1f
            else -> 10.toFloat().pow((value - 72) / 20)
        }
    }

    fun save(preset: Short?, level: Int?): Boolean {
        if (preset != null && level != null) {
            return try {
                effect?.release()
                effect = PresetReverb(100, 0).also {
                    it.preset = preset
                    it.enabled = true
                    playerRepository.setEffect(it, linearInputToLevel(level))
                }
                reverbPreset = preset
                _effectEnabled.value = true
                true
            } catch (e: Exception) {
                unset()
                false
            }
        } else unset()
        return false
    }

    fun unset() {
        reverbPreset = null
        effect?.release()
        effect = null
        _effectEnabled.value = false
        playerRepository.unsetEffect()
    }
}