package us.huseli.soundboard.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.viewmodels.BaseSoundEditViewModel

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
        } ?: run {
            Log.e(LOG_TAG, "onPositiveButtonClick(): binding is null")
        }
        soundViewModel.disableSelect()
        dismiss()
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