package us.huseli.soundboard_kotlin.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel

class DeleteCategoryFragment : DialogFragment() {
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    private val categoryId: Int by lazy { requireArguments().getInt(ARG_CATEGORY_ID) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = categoryListViewModel.get(categoryId)!!

        return AlertDialog.Builder(requireActivity()).run {
            setTitle(R.string.delete_category)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            if (viewModel.soundCount > 0)
                setMessage(R.string.refusing_delete_category_items)
            else {
                setMessage("Delete category ${viewModel.name.value}?")
                setPositiveButton(R.string.ok) { _, _ -> viewModel.delete() }
            }
            create()
        }
    }

    companion object {
        const val ARG_CATEGORY_ID = "categoryId"

        @JvmStatic
        fun newInstance(categoryId: Int): DeleteCategoryFragment {
            return DeleteCategoryFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CATEGORY_ID, categoryId) }
            }
        }
    }
}