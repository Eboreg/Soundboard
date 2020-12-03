package us.huseli.soundboard.fragments

import android.content.Context
import android.os.Bundle
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundEditViewModel

class EditSoundDialogFragment : BaseEditSoundDialogFragment<SoundEditViewModel>() {
    override val title = R.string.edit_sound
    override var viewModel: SoundEditViewModel? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = SoundEditViewModel(requireArguments().getInt(ARG_ID)).apply {
            categoryIndex = requireArguments().getInt(ARG_CATEGORY_INDEX)
        }
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