package us.huseli.soundboard_kotlin.fragments

import android.app.Dialog
import android.os.Bundle
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.viewmodels.SoundAddMultipleViewModel

class AddMultipleSoundDialogFragment(private val sounds: List<Sound>) : BaseSoundDialogFragment<SoundAddMultipleViewModel>() {
    //override var viewModel = SoundAddMultipleViewModel(sounds, requireContext())
    override lateinit var viewModel: SoundAddMultipleViewModel
    override val title = R.string.add_sounds

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = SoundAddMultipleViewModel(sounds, requireContext())
        val dialog = super.onCreateDialog(savedInstanceState)
        binding.soundName.isEnabled = false
        return dialog
    }

    override fun onPositiveButtonClick() {
        viewModel.setVolume(binding.volume.progress)
        viewModel.setCategoryId((binding.category.selectedItem as Category).id!!)
        save()
        dismiss()
    }
}