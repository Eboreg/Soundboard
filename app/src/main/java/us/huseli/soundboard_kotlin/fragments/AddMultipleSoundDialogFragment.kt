package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.viewmodels.SoundAddMultipleViewModel

class AddMultipleSoundDialogFragment : BaseSoundDialogFragment<SoundAddMultipleViewModel>() {
    override val viewModel by activityViewModels<SoundAddMultipleViewModel>()
    override val title = R.string.add_sounds

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.soundName.isEnabled = false
    }

    override fun onPositiveButtonClick() {
        viewModel.setVolume(binding.volume.progress)
        viewModel.setCategoryId((binding.category.selectedItem as Category).id!!)
        save()
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.setVolume(binding.volume.progress)
        viewModel.setCategoryId((binding.category.selectedItem as Category).id!!)
    }
}