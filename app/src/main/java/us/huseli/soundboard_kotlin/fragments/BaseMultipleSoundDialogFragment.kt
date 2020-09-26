package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import android.view.View
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.viewmodels.BaseSoundEditViewModel

abstract class BaseMultipleSoundDialogFragment<VM: BaseSoundEditViewModel> : BaseSoundDialogFragment<VM>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.soundName.isEnabled = false
    }

    override fun onPositiveButtonClick() {
        viewModel.setVolume(binding.volume.progress)
        (binding.category.selectedItem as Category).id?.let { viewModel.setCategoryId(it) }
        save()
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.setVolume(binding.volume.progress)
        (binding.category.selectedItem as Category).id?.let { viewModel.setCategoryId(it) }
    }
}