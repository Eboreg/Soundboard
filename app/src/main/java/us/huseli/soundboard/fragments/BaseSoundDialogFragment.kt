package us.huseli.soundboard.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundViewModel

abstract class BaseSoundDialogFragment : DialogFragment() {
    internal val soundViewModel by activityViewModels<SoundViewModel>()
    internal open val positiveButtonText = R.string.save
    internal open val title: Int? = null

    internal open fun getTitle(): CharSequence? = title?.let { getText(it) }

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

    internal open fun onPositiveButtonClick() {
        soundViewModel.disableSelect()
        dismiss()
    }

    internal open fun configureDialog(builder: MaterialAlertDialogBuilder) {
        builder.apply {
            getTitle()?.let { setTitle(it) }
            getMessage()?.let { setMessage(it) }
            setPositiveButton(positiveButtonText, null)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
        }
    }

    internal open fun getMessage(): CharSequence? = null
}