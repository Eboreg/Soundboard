package us.huseli.soundboard_kotlin.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundAddViewModel

class AddSoundDialogFragment : BaseSoundDialogFragment<SoundAddViewModel>() {
    //override lateinit var viewModel: SoundAddViewModel
    override val viewModel by activityViewModels<SoundAddViewModel>()
    override val title = R.string.add_sound

/*
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.setName(binding.soundName.text.toString())
        viewModel.setVolume(binding.volume.progress)
        viewModel.setCategoryId((binding.category.selectedItem as Category).id!!)
    }
*/
}