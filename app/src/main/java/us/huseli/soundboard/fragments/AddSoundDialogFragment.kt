package us.huseli.soundboard.fragments

import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.activities.BaseActivity
import us.huseli.soundboard.viewmodels.SoundAddViewModel

@AndroidEntryPoint
class AddSoundDialogFragment : BaseEditSoundDialogFragment<SoundAddViewModel>() {
    override val viewModel by activityViewModels<SoundAddViewModel>()
    override val title: Int
        get() = if (viewModel.multiple) R.string.add_sounds else R.string.add_sound

    override fun save() {
        val failFragment = StatusDialogFragment().setTitle(getString(R.string.an_error_occurred))
        viewModel.updateExisting()
        viewModel.soundsToInsert().forEach { sound ->
            try {
                viewModel.insertSound(sound, requireContext())
            } catch (e: Exception) {
                failFragment.addMessage(
                    StatusDialogFragment.Status.ERROR,
                    getString(R.string.could_not_add_sound, sound.name)
                )
                failFragment.addException(e)
            }
        }
        viewModel.pushUndoState()
        dismiss()
        if (failFragment.messageCount > 0)
            (requireActivity() as BaseActivity).showFragment(failFragment)
    }
}