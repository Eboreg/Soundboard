package us.huseli.soundboard_kotlin.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundAddMultipleViewModel

class AddMultipleSoundDialogFragment : BaseEditMultipleSoundDialogFragment<SoundAddMultipleViewModel>() {
    override val viewModel by activityViewModels<SoundAddMultipleViewModel>()
    override var title = R.string.add_sounds
}