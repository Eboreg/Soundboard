package us.huseli.soundboard.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundAddMultipleViewModel

class AddMultipleSoundDialogFragment : BaseEditMultipleSoundDialogFragment<SoundAddMultipleViewModel>() {
    override val viewModel by activityViewModels<SoundAddMultipleViewModel>()
    override val title = R.string.add_sounds
}