package us.huseli.soundboard.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundEditMultipleViewModel

class EditMultipleSoundDialogFragment : BaseEditMultipleSoundDialogFragment<SoundEditMultipleViewModel>() {
    override val viewModel by activityViewModels<SoundEditMultipleViewModel>()
    override val title = R.string.edit_sounds

    override fun getCategories() = categoryListViewModel.categoriesWithEmpty
}