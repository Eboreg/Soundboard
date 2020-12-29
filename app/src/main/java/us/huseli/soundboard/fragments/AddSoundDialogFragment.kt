package us.huseli.soundboard.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundAddViewModel

class AddSoundDialogFragment : BaseEditSoundDialogFragment<SoundAddViewModel>() {
    override val viewModel by activityViewModels<SoundAddViewModel>()
    override val title: Int
        get() = if (viewModel.multiple) R.string.edit_sounds else R.string.edit_sound
}