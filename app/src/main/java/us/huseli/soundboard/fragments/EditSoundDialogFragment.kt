package us.huseli.soundboard.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundEditViewModel

@AndroidEntryPoint
class EditSoundDialogFragment : BaseEditSoundDialogFragment<SoundEditViewModel>() {
    override val viewModel by activityViewModels<SoundEditViewModel>()
    override val title: Int
        get() = if (viewModel.multiple) R.string.edit_sounds else R.string.edit_sound

    override fun getCategories() =
        if (viewModel.multiple) categoryListViewModel.categoriesWithEmpty
        else super.getCategories()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.setCategoryIndex(requireArguments().getInt(ARG_CATEGORY_INDEX))
    }

    override fun save() = viewModel.save()


    companion object {
        @JvmStatic
        fun newInstance(categoryIndex: Int) = EditSoundDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CATEGORY_INDEX, categoryIndex)
            }
        }
    }

}