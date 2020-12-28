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
    private var onAddDuplicateListener: () -> Unit = {}
    private var onSkipDuplicatesListener: () -> Unit = {}
    private var onUpdateExistingListener: () -> Unit = {}
    private val viewModel by activityViewModels<SoundAddViewModel>()

    private fun onAddDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.ADD
        (requireActivity() as? MainActivity)?.apply {
            when (viewModel.sounds.size) {
                1 -> showDialogFragment(AddSoundDialogFragment())
                else -> showDialogFragment(AddMultipleSoundDialogFragment())
            }
        }
    }

    private fun onSkipDuplicates() {
        viewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.SKIP
        (requireActivity() as? MainActivity)?.apply {
            when (viewModel.sounds.size) {
                0 -> showSnackbar(R.string.no_sounds_to_add)
                1 -> showDialogFragment(AddSoundDialogFragment())
                else -> showDialogFragment(AddMultipleSoundDialogFragment())
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
                else -> showDialogFragment(AddMultipleSoundDialogFragment())
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val binding = FragmentAddDuplicateSoundBinding.inflate(inflater).also { it.viewModel = viewModel }
        return MaterialAlertDialogBuilder(requireContext(), R.style.Soundboard_Theme_AlertDialog_DuplicateSound).run {
            setView(binding.root)
            setTitle(resources.getQuantityString(R.plurals.duplicate_sound_added, viewModel.duplicateCount))
            setPositiveButton(resources.getQuantityString(R.plurals.add_duplicate, viewModel.duplicateCount)) { _, _ ->
                onAddDuplicates()
                onAddDuplicateListener()
            }
            setNeutralButton(R.string.update_existing) { _, _ ->
                onUpdateExisting()
                onUpdateExistingListener()
            }
            // if (arguments?.getBoolean(ARG_SHOW_SKIP) == true)
            if (viewModel.sounds.size > 1)
                setNegativeButton(R.string.skip) { _, _ ->
                    onSkipDuplicates()
                    onSkipDuplicatesListener()
                }
            create()
        }
    }

    fun setOnAddDuplicateListener(listener: () -> Unit) {
        onAddDuplicateListener = listener
    }

    fun setOnSkipDuplicatesListener(listener: () -> Unit) {
        onSkipDuplicatesListener = listener
    }

    fun setOnUpdateExistingListener(listener: () -> Unit) {
        onUpdateExistingListener = listener
    }


    companion object {
        const val ARG_SHOW_SKIP = "showSkipButton"

        @JvmStatic
        fun newInstance(showSkipButton: Boolean) = AddDuplicateSoundDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SHOW_SKIP, showSkipButton)
            }
        }
    }
}