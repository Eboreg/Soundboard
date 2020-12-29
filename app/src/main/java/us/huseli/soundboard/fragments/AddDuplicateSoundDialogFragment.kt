package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.MainActivity
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentAddDuplicateSoundBinding
import us.huseli.soundboard.viewmodels.SoundAddViewModel

class AddDuplicateSoundDialogFragment : DialogFragment() {
    private val viewModel by activityViewModels<SoundAddViewModel>()

    private fun onAddDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.ADD
        (requireActivity() as? MainActivity)?.apply {
            if (viewModel.sounds.size > 1) viewModel.setName(getString(R.string.multiple_sounds_selected, viewModel.sounds.size))
            showDialogFragment(AddSoundDialogFragment())
        }
    }

    private fun onSkipDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.SKIP
        (requireActivity() as? MainActivity)?.apply {
            if (viewModel.sounds.isEmpty()) showSnackbar(R.string.no_sounds_to_add)
            else {
                if (viewModel.sounds.size > 1) viewModel.setName(getString(R.string.multiple_sounds_selected, viewModel.sounds.size))
                showDialogFragment(AddSoundDialogFragment())
            }
        }
    }

    private fun onUpdateExisting() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.UPDATE
        (requireActivity() as? MainActivity)?.apply {
            when (viewModel.sounds.size) {
                // TODO: Will not work with copying of data
                // Have to use old sound from duplicates, and not new one from sounds:
                1 -> showEditSoundDialogFragment(viewModel.duplicates[0])
                else -> {
                    viewModel.setName(getString(R.string.multiple_sounds_selected, viewModel.sounds.size))
                    showDialogFragment(AddSoundDialogFragment())
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val binding = FragmentAddDuplicateSoundBinding.inflate(inflater).also { it.viewModel = viewModel }
        return MaterialAlertDialogBuilder(requireContext(), R.style.Soundboard_Theme_AlertDialog_DuplicateSound).run {
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