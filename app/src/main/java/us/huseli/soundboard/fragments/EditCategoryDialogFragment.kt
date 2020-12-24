package us.huseli.soundboard.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.BaseCategoryEditViewModel
import us.huseli.soundboard.viewmodels.CategoryListViewModel

class EditCategoryDialogFragment : BaseCategoryDialogFragment() {
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    private val categoryId by lazy { requireArguments().getInt(ARG_ID) }

    override var viewModel: BaseCategoryEditViewModel? = null
    override val title = R.string.edit_category

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return try {
            viewModel = categoryListViewModel.getCategoryEditViewModel(categoryId)
            super.onCreateDialog(savedInstanceState)
        } catch (e: NullPointerException) {
            MaterialAlertDialogBuilder(requireContext()).run {
                setMessage(R.string.data_not_fetched_yet)
                create()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(categoryId: Int, dialogId: Int): EditCategoryDialogFragment {
            return EditCategoryDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, categoryId)
                    putInt(ARG_DIALOG_ID, dialogId)
                }
            }
        }
    }
}