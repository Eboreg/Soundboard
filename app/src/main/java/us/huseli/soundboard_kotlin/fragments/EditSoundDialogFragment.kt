package us.huseli.soundboard_kotlin.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundEditViewModel

class EditSoundDialogFragment : BaseSoundDialogFragment<SoundEditViewModel>() {
    private val appViewModel by activityViewModels<AppViewModel>()
    override val title = R.string.edit_sound
    override lateinit var viewModel: SoundEditViewModel

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        appViewModel.disableSelect()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = SoundEditViewModel(requireArguments().getInt(ARG_ID))
        viewModel.categoryIndex = requireArguments().getInt(ARG_CATEGORY_INDEX)
    }

    companion object {
        @JvmStatic
        fun newInstance(soundId: Int, categoryIndex: Int) = EditSoundDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_ID, soundId)
                putInt(ARG_CATEGORY_INDEX, categoryIndex)
            }
        }
    }
}