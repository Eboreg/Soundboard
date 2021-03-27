package us.huseli.soundboard.fragments

import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentAudioEffectDialogBinding
import us.huseli.soundboard.viewmodels.AudioEffectViewModel

@AndroidEntryPoint
class AudioEffectDialogFragment : DialogFragment() {
    private var binding: FragmentAudioEffectDialogBinding? = null
    private val presetItems = listOf(
        PresetItem(PresetReverb.PRESET_PLATE, R.string.reverb_plate),
        PresetItem(PresetReverb.PRESET_SMALLROOM, R.string.reverb_small_room),
        PresetItem(PresetReverb.PRESET_MEDIUMROOM, R.string.reverb_medium_room),
        PresetItem(PresetReverb.PRESET_LARGEROOM, R.string.reverb_large_room),
        PresetItem(PresetReverb.PRESET_MEDIUMHALL, R.string.reverb_medium_hall),
        PresetItem(PresetReverb.PRESET_LARGEHALL, R.string.reverb_large_hall),
    )
    private val viewModel by activityViewModels<AudioEffectViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentAudioEffectDialogBinding.inflate(inflater).also { binding ->
            binding.preset.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_item, presetItems)
            viewModel.reverbPreset?.let { preset ->
                binding.preset.setSelection(presetItems.map { it.value }.indexOf(preset))
            }
            binding.viewModel = viewModel
        }

        return MaterialAlertDialogBuilder(requireContext(),
            R.style.Soundboard_Theme_MaterialAlertDialog).run {
            setTitle(R.string.set_reverb)
            binding?.let { binding ->
                setView(binding.root)
                setPositiveButton("OK") { _, _ ->
                    if (!viewModel.save((binding.preset.selectedItem as? PresetItem)?.value,
                            binding.sendLevel.progress))
                        Snackbar.make(binding.root, "Could not enable effect", Snackbar.LENGTH_SHORT).show()
                }
            }
            setNeutralButton("Remove effect") { _, _ -> viewModel.unset() }
            setNegativeButton("Cancel") { _, _ -> }
            create()
        }
    }

    inner class PresetItem(val value: Short, val stringRes: Int) {
        override fun toString() = getString(stringRes)
    }
}