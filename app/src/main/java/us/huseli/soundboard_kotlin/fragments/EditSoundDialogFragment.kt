package us.huseli.soundboard_kotlin.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

class EditSoundDialogFragment : BaseSoundDialogFragment() {
    private val soundId: Int by lazy { requireArguments().getInt(ARG_SOUND_ID) }
    private val soundListViewModel by activityViewModels<SoundListViewModel>()
    override lateinit var viewModel: SoundViewModel
    override val title = R.string.edit_sound

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = soundListViewModel.getSoundViewModel(soundId)!!
    }

    companion object {
        const val ARG_SOUND_ID = "soundId"

        @JvmStatic
        fun newInstance(soundId: Int) = EditSoundDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SOUND_ID, soundId)
            }
        }
    }
}