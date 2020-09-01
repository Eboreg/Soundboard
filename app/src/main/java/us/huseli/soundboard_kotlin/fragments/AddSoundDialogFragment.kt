package us.huseli.soundboard_kotlin.fragments

import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.viewmodels.SoundAddViewModel

class AddSoundDialogFragment(sound: Sound) : BaseSoundDialogFragment<SoundAddViewModel>() {
    override var viewModel = SoundAddViewModel(sound)
    override val title = R.string.add_sound

    override fun save() {
        // If sounds exist, set sound.order to max order + 1; else 0
        val lastSound = sounds.maxByOrNull { it.order }
        lastSound?.order?.let {
            viewModel.setOrder(it + 1)
        }
        super.save()
    }
}