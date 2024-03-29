package us.huseli.soundboard.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundViewModel

@AndroidEntryPoint
abstract class BaseSoundDialogFragment : DialogFragment() {
    protected val soundViewModel by activityViewModels<SoundViewModel>()
    protected open val positiveButtonText = R.string.save
    protected open val title: Int? = null


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
    protected open fun configureDialog(builder: MaterialAlertDialogBuilder) {
        builder.apply {
            getTitle()?.let { setTitle(it) }
            getMessage()?.let { setMessage(it) }
            setPositiveButton(positiveButtonText, null)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
        }
    }

    protected open fun getMessage(): CharSequence? = null

    protected open fun getTitle(): CharSequence? = title?.let { getText(it) }

    protected open fun onPositiveButtonClick() {
        soundViewModel.disableSelect()
        dismiss()
    }

}