package us.huseli.soundboard_kotlin.fragments

import android.content.Context
import android.os.Bundle
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundEditViewModel

class EditSoundDialogFragment : BaseEditSoundDialogFragment<SoundEditViewModel>() {
    override var title = R.string.edit_sound
    override lateinit var viewModel: SoundEditViewModel

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