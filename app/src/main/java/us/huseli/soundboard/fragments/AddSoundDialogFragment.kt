package us.huseli.soundboard.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundAddViewModel

class AddSoundDialogFragment : BaseEditSoundDialogFragment<SoundAddViewModel>() {
    override val viewModel by activityViewModels<SoundAddViewModel>()
    override val title = R.string.add_sound
}