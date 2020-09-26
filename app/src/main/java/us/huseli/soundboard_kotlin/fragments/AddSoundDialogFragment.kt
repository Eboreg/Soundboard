package us.huseli.soundboard_kotlin.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundAddViewModel

class AddSoundDialogFragment : BaseSoundDialogFragment<SoundAddViewModel>() {
    override val viewModel by activityViewModels<SoundAddViewModel>()
    override val title = R.string.add_sound
}