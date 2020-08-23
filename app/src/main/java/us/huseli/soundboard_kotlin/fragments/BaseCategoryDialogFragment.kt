package us.huseli.soundboard_kotlin.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.fragment_edit_category.*
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.databinding.FragmentEditCategoryBinding
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.viewmodels.CategoryEditViewModel

abstract class BaseCategoryDialogFragment : DialogFragment(), ColorPickerDialogListener {
    private val colorHelper by lazy { ColorHelper(requireContext()) }
    private val dialogId by lazy { requireArguments().getInt(ARG_DIALOG_ID) }

    internal abstract var viewModel: CategoryEditViewModel
    internal abstract val title: Int
    internal lateinit var binding: FragmentEditCategoryBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.getString(ARG_NAME)?.let { viewModel.name = it }

        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditCategoryBinding.inflate(inflater, edit_sound_category_fragment, false)
        binding.viewModel = viewModel

        return AlertDialog.Builder(requireContext()).run {
            setTitle(title)
            setView(binding.root)
            setPositiveButton(R.string.save) { _, _ ->
                val catName = binding.categoryName.text.toString().trim()
                if (catName.isEmpty())
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                else
                    viewModel.name = catName
                viewModel.save()
            }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            binding.selectColourButton.setOnClickListener { onSelectColourClick() }
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

    override fun onColorSelected(dialogId: Int, color: Int) {
        viewModel.setBackgroundColor(color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit

    private fun onSelectColourClick() {
        ColorPickerDialog.newBuilder().apply {
            setPresets(colorHelper.colors.toIntArray())
            setDialogTitle(R.string.select_background_colour)
            viewModel.backgroundColor.value?.let { setColor(it) }
            setDialogId(dialogId)
            show(requireActivity())
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_ORDER = "order"
        const val ARG_DIALOG_ID = "dialogId"  // for ColorPickerDialog
        // For restoring instance state on recreate
        const val ARG_NAME = "name"
    }
}