package us.huseli.soundboard.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel

@AndroidEntryPoint
abstract class BaseSoundDialogFragment : DialogFragment() {
    internal val appViewModel by activityViewModels<AppViewModel>()
    internal val soundViewModel by activityViewModels<SoundViewModel>()
    internal open val positiveButtonText = R.string.save
    internal open val title: Int? = null


    /** OVERRIDDEN STANDARD METHODS */
    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(requireContext()).run {
            configureDialog(this)
            create()
        }
        // Custom listener to avoid automatic dismissal!
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onPositiveButtonClick() }
        }
        return dialog
    }


    /** OWN METHODS */
    internal open fun configureDialog(builder: MaterialAlertDialogBuilder) {
        builder.apply {
            getTitle()?.let { setTitle(it) }
            getMessage()?.let { setMessage(it) }
            setPositiveButton(positiveButtonText, null)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
        }
    }

    internal open fun getMessage(): CharSequence? = null

    internal open fun getTitle(): CharSequence? = title?.let { getText(it) }

    internal open fun onPositiveButtonClick() {
        soundViewModel.disableSelect()
        dismiss()
    }

}