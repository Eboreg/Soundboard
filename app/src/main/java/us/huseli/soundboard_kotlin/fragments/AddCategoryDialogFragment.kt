package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.CategoryEditViewModel

class AddCategoryDialogFragment : BaseCategoryDialogFragment() {
    private val order by lazy { arguments?.getInt(ARG_ORDER) }

    override var viewModel = CategoryEditViewModel(null, order)
    override val title = R.string.add_category

    companion object {
        @JvmStatic
        fun newInstance(order: Int, dialogId: Int): AddCategoryDialogFragment {
            return AddCategoryDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ORDER, order)
                    putInt(ARG_DIALOG_ID, dialogId)
                }
            }
        }
    }
}