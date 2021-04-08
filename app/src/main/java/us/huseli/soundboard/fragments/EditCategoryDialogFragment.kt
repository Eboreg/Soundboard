package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.viewmodels.CategoryEditViewModel

@AndroidEntryPoint
class EditCategoryDialogFragment : BaseCategoryDialogFragment() {
    private val categoryId by lazy { requireArguments().getInt(ARG_ID) }
    private var sortOrder = Sound.SortOrder.ASCENDING
    private val sortParameterItems = listOf(
        SortParameterItem(Sound.SortParameter.UNDEFINED, R.string.unchanged),
        SortParameterItem(Sound.SortParameter.NAME, R.string.name),
        SortParameterItem(Sound.SortParameter.DURATION, R.string.duration),
        SortParameterItem(Sound.SortParameter.TIME_ADDED, R.string.add_date),
    )

    override val viewModel by viewModels<CategoryEditViewModel>()
    override val title = R.string.edit_category

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return try {
            viewModel.setCategoryId(categoryId)
            val dialog = super.onCreateDialog(savedInstanceState)
            binding?.also {
                it.sortContainer.visibility = View.VISIBLE
                it.sortOrder.check(it.sortOrderAscending.id)
                it.sortOrder.setOnCheckedChangeListener { _, checkedId ->
                    sortOrder = when (checkedId) {
                        it.sortOrderDescending.id -> Sound.SortOrder.DESCENDING
                        else -> Sound.SortOrder.ASCENDING
                    }
                }

                it.sortBy.adapter = ArrayAdapter(
                    requireContext(), android.R.layout.simple_spinner_item, sortParameterItems)
            }
            dialog
        } catch (e: NullPointerException) {
            MaterialAlertDialogBuilder(requireContext()).run {
                setMessage(R.string.data_not_fetched_yet)
                create()
            }
        }
    }

    override fun save() {
        val sortBy = (binding?.sortBy?.selectedItem as? SortParameterItem)?.value

        val soundSorting = if (sortBy != null && sortBy != Sound.SortParameter.UNDEFINED)
            Sound.Sorting(sortBy, sortOrder) else null

        viewModel.apply {
            setName(binding?.categoryName?.text.toString().trim())
            save(soundSorting)
            dismiss()
        }
    }


    inner class SortParameterItem(val value: Sound.SortParameter, val stringRes: Int) {
        override fun toString() = getString(stringRes)
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