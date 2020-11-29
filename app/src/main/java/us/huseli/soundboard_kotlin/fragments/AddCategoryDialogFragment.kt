package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.BaseCategoryEditViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryAddViewModel

class AddCategoryDialogFragment : BaseCategoryDialogFragment() {
    override var viewModel: BaseCategoryEditViewModel? = CategoryAddViewModel()
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