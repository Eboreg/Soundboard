package us.huseli.soundboard_kotlin.fragments

import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.viewmodels.SoundAddViewModel

class AddSoundDialogFragment(sound: Sound) : BaseSoundDialogFragment<SoundAddViewModel>() {
    override var viewModel = SoundAddViewModel(sound)
    override val title = R.string.add_sound
}