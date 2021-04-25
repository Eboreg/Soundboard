package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.data.SoundSorting
import us.huseli.soundboard.viewmodels.CategoryEditViewModel

@AndroidEntryPoint
class EditCategoryDialogFragment : BaseCategoryDialogFragment(), RadioGroup.OnCheckedChangeListener,
    AdapterView.OnItemSelectedListener {
    private val sortParameterItems = listOf(
        SortParameterItem(SoundSorting.Parameter.UNDEFINED, R.string.unchanged),
        SortParameterItem(SoundSorting.Parameter.NAME, R.string.name),
        SortParameterItem(SoundSorting.Parameter.DURATION, R.string.duration),
        SortParameterItem(SoundSorting.Parameter.TIME_ADDED, R.string.add_date),
    )

    override val viewModel by activityViewModels<CategoryEditViewModel>()
    override val title = R.string.edit_category

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return try {
            val dialog = super.onCreateDialog(savedInstanceState)
            binding.sortContainer.visibility = View.VISIBLE
            binding.sortOrder.check(binding.sortOrderAscending.id)
            binding.sortOrder.setOnCheckedChangeListener(this)
            binding.sortBy.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_item, sortParameterItems)
            binding.sortBy.onItemSelectedListener = this
            dialog
        } catch (e: NullPointerException) {
            // TODO: When would this happen?
            MaterialAlertDialogBuilder(requireContext()).run {
                setMessage(R.string.data_not_fetched_yet)
                create()
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        if (group != null && group == binding.sortOrder) {
            viewModel.sortOrder = when (checkedId) {
                binding.sortOrderDescending.id -> SoundSorting.Order.DESCENDING
                else -> SoundSorting.Order.ASCENDING
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (parent != null && parent == binding.sortBy)
            (parent.getItemAtPosition(position) as? SortParameterItem)?.let { viewModel.sortParameter = it.value }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        if (parent != null && parent == binding.sortBy) viewModel.sortParameter = null
    }


    inner class SortParameterItem(val value: SoundSorting.Parameter, val stringRes: Int) {
        override fun toString() = getString(stringRes)
    }


    companion object {
        @JvmStatic
        fun newInstance(dialogId: Int): EditCategoryDialogFragment {
            return EditCategoryDialogFragment().apply {
                arguments = Bundle().apply { putInt(ARG_DIALOG_ID, dialogId) }
            }
        }
    }
}