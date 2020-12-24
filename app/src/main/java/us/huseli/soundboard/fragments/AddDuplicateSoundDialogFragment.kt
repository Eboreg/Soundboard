package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.FragmentAddDuplicateSoundBinding
import us.huseli.soundboard.viewmodels.SoundAddViewModel

class AddDuplicateSoundDialogFragment : DialogFragment() {
    private var onAddDuplicateListener: () -> Unit = {}
    private var onSkipDuplicatesListener: () -> Unit = {}
    private var onUpdateExistingListener: (Sound?) -> Unit = {}
    private val viewModel by activityViewModels<SoundAddViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        val binding = FragmentAddDuplicateSoundBinding.inflate(inflater).also { it.viewModel = viewModel }
        return MaterialAlertDialogBuilder(requireContext()).run {
            setView(binding.root)
            setTitle(resources.getQuantityString(R.plurals.duplicate_sound_added, viewModel.duplicateCount))
            setPositiveButton(resources.getQuantityString(R.plurals.add_duplicate, viewModel.duplicateCount)) { _, _ ->
                onAddDuplicateListener()
            }
            setNegativeButton(R.string.update_existing) { _, _ -> onUpdateExistingListener(viewModel.duplicate) }
            if (arguments?.getBoolean(ARG_SHOW_SKIP) == true)
                setNeutralButton(R.string.skip) { _, _ -> onSkipDuplicatesListener() }
            create()
        }
    }

    fun setOnAddDuplicateListener(listener: () -> Unit) {
        onAddDuplicateListener = listener
    }

    fun setOnSkipDuplicatesListener(listener: () -> Unit) {
        onSkipDuplicatesListener = listener
    }

    fun setOnUpdateExistingListener(listener: (Sound?) -> Unit) {
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