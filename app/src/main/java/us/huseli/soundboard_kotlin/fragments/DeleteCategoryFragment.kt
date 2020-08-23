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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = categoryListViewModel.getCategoryViewModel(requireArguments().getInt(ARG_CATEGORY_ID))!!

        return AlertDialog.Builder(requireContext()).run {
            setTitle(R.string.delete_category)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            when(viewModel.soundCount) {
                0 -> setMessage(resources.getString(R.string.delete_category_with_name, viewModel.name.value))
                1 -> setMessage(resources.getString(R.string.delete_category_with_name_and_sound, viewModel.name.value))
                else -> setMessage(resources.getString(R.string.delete_category_with_name_and_sounds, viewModel.name.value, viewModel.soundCount))
            }
            setPositiveButton(R.string.ok) { _, _ -> viewModel.delete() }
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