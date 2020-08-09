package us.huseli.soundboard_kotlin.fragments

import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

class AddSoundDialogFragment(sound: Sound) : BaseSoundDialogFragment() {
    //override var viewModel by viewModels<SoundViewModel> { SoundViewModelFactory(sound) }
    override var viewModel = SoundViewModel(sound)
    override val title = R.string.add_sound
}