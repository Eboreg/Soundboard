package us.huseli.soundboard.fragments

import android.os.Bundle
import androidx.fragment.app.viewModels
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.CategoryAddViewModel

class AddCategoryDialogFragment : BaseCategoryDialogFragment() {
    override val viewModel by viewModels<CategoryAddViewModel>()
    override val title = R.string.add_category

    companion object {
        @JvmStatic
        fun newInstance(dialogId: Int): AddCategoryDialogFragment {
            return AddCategoryDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DIALOG_ID, dialogId)
                }
            }
        }
    }
}