package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.viewmodels.BaseSoundEditViewModel

abstract class BaseEditMultipleSoundDialogFragment<VM: BaseSoundEditViewModel> : BaseEditSoundDialogFragment<VM>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.soundName?.isEnabled = false
    }

    override fun onPositiveButtonClick() {
        binding?.let { binding ->
            viewModel?.let { viewModel ->
                viewModel.setVolume(binding.volume.progress)
                (binding.category.selectedItem as Category).id?.let { viewModel.setCategoryId(it) }
                save()
            } ?: run {
                Log.e(LOG_TAG, "onPositiveButtonClick(): viewModel is null")
            }
            dismiss()
        } ?: run {
            Log.e(LOG_TAG, "onPositiveButtonClick(): binding is null")
            dismiss()
        }
        soundViewModel.disableSelect()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel?.let { viewModel ->
            binding?.let { binding ->
                viewModel.setVolume(binding.volume.progress)
                (binding.category.selectedItem as Category).id?.let { viewModel.setCategoryId(it) }
            }
        }
    }
}