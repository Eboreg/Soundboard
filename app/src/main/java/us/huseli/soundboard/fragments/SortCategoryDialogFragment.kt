package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.FragmentSortCategoryBinding
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel

class SortCategoryDialogFragment : DialogFragment() {
    private var binding: FragmentSortCategoryBinding? = null

    private val appViewModel by activityViewModels<AppViewModel>()
    private val categoryId by lazy { requireArguments().getInt(ARG_ID) }
    private val name by lazy { requireArguments().getString(ARG_NAME) }
    private val soundViewModel by activityViewModels<SoundViewModel>()

    private var sortBy: Sound.SortParameter? = null
    private var sortOrder = Sound.SortOrder.ASCENDING

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentSortCategoryBinding.inflate(inflater).also {
            it.sortOrder.check(it.sortOrderAscending.id)
            it.sortOrder.setOnCheckedChangeListener { _, checkedId ->
                sortOrder = when (checkedId) {
                    it.sortOrderDescending.id -> Sound.SortOrder.DESCENDING
                    else -> Sound.SortOrder.ASCENDING
                }
            }
            it.sortBy.setOnCheckedChangeListener { _, checkedId ->
                sortBy = when (checkedId) {
                    it.sortByName.id -> Sound.SortParameter.NAME
                    it.sortByAddDate.id -> Sound.SortParameter.TIME_ADDED
                    it.sortByDuration.id -> Sound.SortParameter.DURATION
                    else -> null
                }?.also { (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = true }
            }
        }
        val dialog = MaterialAlertDialogBuilder(requireContext()).run {
            setTitle(resources.getString(R.string.sort_category_dialog_title, name))
            setView(binding?.root)
            setPositiveButton(R.string.sort) { _, _ ->
                sortBy?.let { sortBy ->
                    appViewModel.pushSoundUndoState()
                    soundViewModel.sort(categoryId, sortBy, sortOrder)
                }
            }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            create()
        }

        dialog.setOnShowListener {
            if (sortBy == null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }

        return dialog
    }


    companion object {
        const val ARG_ID = "id"
        const val ARG_NAME = "name"

        @JvmStatic
        fun newInstance(categoryId: Int, categoryName: String): SortCategoryDialogFragment {
            return SortCategoryDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, categoryId)
                    putString(ARG_NAME, categoryName)
                }
            }
        }
    }
}
