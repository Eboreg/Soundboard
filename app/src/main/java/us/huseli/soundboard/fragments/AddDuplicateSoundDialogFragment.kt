@file:Suppress("Annotator")

package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentAddDuplicateSoundBinding
import us.huseli.soundboard.interfaces.EditSoundInterface
import us.huseli.soundboard.interfaces.SnackbarInterface
import us.huseli.soundboard.viewmodels.SoundAddViewModel

@AndroidEntryPoint
class AddDuplicateSoundDialogFragment : DialogFragment() {
    private val viewModel by activityViewModels<SoundAddViewModel>()

    private fun onAddDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.ADD
        (requireActivity() as EditSoundInterface).apply {
            if (viewModel.sounds.size > 1) viewModel.setName(getString(R.string.multiple_sounds_selected, viewModel.sounds.size))
            showSoundAddDialog()
        }
    }

    private fun onSkipDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.SKIP
        if (viewModel.sounds.isEmpty()) (requireActivity() as SnackbarInterface).showSnackbar(R.string.no_sounds_to_add)
        else {
            if (viewModel.sounds.size > 1) viewModel.setName(getString(R.string.multiple_sounds_selected, viewModel.sounds.size))
            (requireActivity() as EditSoundInterface).showSoundAddDialog()
        }
    }

    private fun onUpdateExisting() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.UPDATE
        (requireActivity() as EditSoundInterface).apply {
            when (viewModel.sounds.size) {
                // TODO: Will not work with copying of data
                // Have to use old sound from duplicates, and not new one from sounds:
                1 -> showSoundEditDialog(viewModel.duplicates[0])
                else -> {
                    viewModel.setName(getString(R.string.multiple_sounds_selected, viewModel.sounds.size))
                    showSoundAddDialog()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val binding = FragmentAddDuplicateSoundBinding.inflate(inflater).also { it.viewModel = viewModel }
        return MaterialAlertDialogBuilder(requireContext(), R.style.Soundboard_Theme_MaterialAlertDialog_EqualButtons).run {
            setView(binding.root)
            setTitle(resources.getQuantityString(R.plurals.duplicate_sound_added, viewModel.duplicateCount))
            setPositiveButton(resources.getQuantityString(R.plurals.add_duplicate, viewModel.duplicateCount)) { _, _ -> onAddDuplicates() }
            setNeutralButton(R.string.update_existing) { _, _ -> onUpdateExisting() }
            if (viewModel.sounds.size > 1)
                setNegativeButton(R.string.skip) { _, _ -> onSkipDuplicates() }
            create()
        }
    }
}