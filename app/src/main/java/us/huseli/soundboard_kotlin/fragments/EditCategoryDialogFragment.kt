package us.huseli.soundboard_kotlin.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.fragment_edit_category.*
import kotlinx.android.synthetic.main.fragment_edit_category.view.*
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.databinding.FragmentEditCategoryBinding
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryEditViewModel

class EditCategoryDialogFragment : DialogFragment(), ColorPickerDialogListener {
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    private val categoryId by lazy {
        arguments?.let { if (it.containsKey(ARG_ID)) it.getInt(ARG_ID) else null }
    }
    private val colorHelper by lazy { ColorHelper(requireContext()) }
    private val order by lazy { arguments?.getInt(ARG_ORDER) }
    private val dialogId by lazy { arguments?.getInt(ARG_DIALOG_ID) }

    private lateinit var editViewModel: CategoryEditViewModel
    private lateinit var binding: FragmentEditCategoryBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        editViewModel = categoryId?.let { categoryListViewModel.getEditViewModel(it)!! } ?: CategoryEditViewModel(null, order)
        savedInstanceState?.getString(ARG_NAME)?.let { editViewModel.name = it }

        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditCategoryBinding.inflate(inflater, edit_sound_category_fragment, false)
        binding.viewModel = editViewModel
        val view = binding.root

        return AlertDialog.Builder(requireActivity()).run {
            when (categoryId) {
                null -> setTitle(R.string.add_category)
                else -> setTitle(R.string.edit_category)
            }
            setView(view)
            setPositiveButton(R.string.save) { _, _ ->
                val catName = view.category_name.text.toString().trim()
                if (catName.isEmpty())
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                else
                    editViewModel.name = catName
                    editViewModel.save()
                }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            view.select_colour_button.setOnClickListener { onSelectColourClick() }
            create()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARG_NAME, binding.categoryName.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
    }

    private fun onSelectColourClick() {
        ColorPickerDialog.newBuilder().apply {
            setPresets(colorHelper.colors.toIntArray())
            setDialogTitle(R.string.select_background_colour)
            editViewModel.backgroundColor.value?.let { setColor(it) }
            dialogId?.let { setDialogId(it) }
            show(requireActivity())
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        editViewModel.setBackgroundColor(color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit

    companion object {
        const val ARG_ID = "id"
        const val ARG_ORDER = "order"
        const val ARG_DIALOG_ID = "dialogId"  // for ColorPickerDialog
        // For restoring instance state on recreate
        const val ARG_NAME = "name"

        @JvmStatic
        fun newInstance(categoryId: Int?, order: Int?, dialogId: Int?): EditCategoryDialogFragment {
            return EditCategoryDialogFragment().apply {
                arguments = Bundle().apply {
                    if (categoryId != null) putInt(ARG_ID, categoryId)
                    if (order != null) putInt(ARG_ORDER, order)
                    if (dialogId != null) putInt(ARG_DIALOG_ID, dialogId)
                }
            }
        }
    }
}