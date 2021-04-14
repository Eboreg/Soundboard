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
        if (viewModel.multiple)
            viewModel.name = getString(R.string.multiple_sounds_selected, viewModel.soundCount)
        (requireActivity() as EditSoundInterface).showSoundAddDialog()
    }

    private fun onSkipDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.SKIP
        if (viewModel.soundCount == 0) (requireActivity() as SnackbarInterface).showSnackbar(R.string.no_sounds_to_add)
        else {
            if (viewModel.multiple)
                viewModel.name = getString(R.string.multiple_sounds_selected, viewModel.soundCount)
            (requireActivity() as EditSoundInterface).showSoundAddDialog()
        }
    }

    private fun onUpdateExisting() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.UPDATE
        (requireActivity() as EditSoundInterface).apply {
            when {
                viewModel.multiple -> {
                    viewModel.name = getString(R.string.multiple_sounds_selected, viewModel.soundCount)
                    showSoundAddDialog()
                }
                viewModel.hasDuplicates -> showSoundEditDialog(viewModel.duplicates.first())
                else -> showSoundAddDialog()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val binding = FragmentAddDuplicateSoundBinding.inflate(inflater).also { it.viewModel = viewModel }
        return MaterialAlertDialogBuilder(requireContext(),
            R.style.Soundboard_Theme_MaterialAlertDialog_EqualButtons).run {
            setView(binding.root)
            setTitle(resources.getQuantityString(R.plurals.duplicate_sound_selected, viewModel.duplicateCount))
            setPositiveButton(resources.getQuantityString(R.plurals.add_duplicate,
                viewModel.duplicateCount)) { _, _ -> onAddDuplicates() }
            setNeutralButton(R.string.update_existing) { _, _ -> onUpdateExisting() }
            if (viewModel.multiple) setNegativeButton(R.string.skip) { _, _ -> onSkipDuplicates() }
            create()
        }
    }
}