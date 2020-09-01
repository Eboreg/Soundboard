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
        return AlertDialog.Builder(requireContext()).run {
            val name = requireArguments().getString(ARG_NAME) ?: ""
            val soundCount = requireArguments().getInt(ARG_SOUNDCOUNT)
            val id = requireArguments().getInt(ARG_ID)

            setTitle(R.string.delete_category)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            when(soundCount) {
                0 -> setMessage(resources.getString(R.string.delete_category_with_name, name))
                else -> setMessage(resources.getQuantityString(R.plurals.delete_category_with_name_and_sounds, soundCount, soundCount))
            }
            setPositiveButton(R.string.ok) { _, _ -> categoryListViewModel.delete(id) }
            create()
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_NAME = "name"
        const val ARG_SOUNDCOUNT = "soundCount"

        @JvmStatic
        fun newInstance(id: Int, name: String, soundCount: Int): DeleteCategoryFragment {
            return DeleteCategoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, id)
                    putInt(ARG_SOUNDCOUNT, soundCount)
                    putString(ARG_NAME, name)
                }
            }
        }
    }
}