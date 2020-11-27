package us.huseli.soundboard_kotlin.fragments

import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundEditMultipleViewModel

class EditMultipleSoundDialogFragment : BaseEditMultipleSoundDialogFragment<SoundEditMultipleViewModel>() {
    override val viewModel by activityViewModels<SoundEditMultipleViewModel>()
    override val title = R.string.edit_sounds

    override fun getCategories() = categoryListViewModel.categoriesWithEmpty
}