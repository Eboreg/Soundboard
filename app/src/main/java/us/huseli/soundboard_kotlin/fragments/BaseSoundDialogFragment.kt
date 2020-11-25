package us.huseli.soundboard_kotlin.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

abstract class BaseSoundDialogFragment : DialogFragment() {
    internal val soundViewModel by activityViewModels<SoundViewModel>()
    internal open val positiveButtonText = R.string.save
    internal abstract var title: Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).run {
            configureDialog(this)
            create()
        }
    }

    internal open fun onPositiveButtonClick() = soundViewModel.disableSelect()

    internal open fun configureDialog(builder: AlertDialog.Builder) {
        builder.apply {
            setTitle(title)
            setPositiveButton(positiveButtonText) { _, _ -> onPositiveButtonClick() }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
        }
    }

}