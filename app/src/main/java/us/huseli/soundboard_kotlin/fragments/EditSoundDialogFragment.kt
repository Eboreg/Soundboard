package us.huseli.soundboard_kotlin.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundEditViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel

class EditSoundDialogFragment : BaseSoundDialogFragment() {
    private val soundListViewModel by activityViewModels<SoundListViewModel>()

    override val title = R.string.edit_sound
    override lateinit var viewModel: SoundEditViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = soundListViewModel.getSoundEditViewModel(requireArguments().getInt(ARG_ID))!!
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