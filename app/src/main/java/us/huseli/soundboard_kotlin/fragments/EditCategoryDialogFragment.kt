package us.huseli.soundboard_kotlin.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableInt
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_edit_category.*
import kotlinx.android.synthetic.main.fragment_edit_category.view.*
import petrov.kristiyan.colorpicker.ColorPicker
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.databinding.FragmentEditCategoryBinding
import us.huseli.soundboard_kotlin.helpers.ColorHelper
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel

class EditCategoryDialogFragment : DialogFragment() {
    private val backgroundColor = ObservableInt(Color.DKGRAY)
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    private val categoryId by lazy {
        arguments?.let { if (it.containsKey(ARG_CATEGORY_ID)) it.getInt(ARG_CATEGORY_ID) else null }
    }
    private val colorHelper by lazy { ColorHelper(requireContext()) }
    private val order by lazy { arguments?.getInt(ARG_ORDER) }

    private lateinit var viewModel: CategoryViewModel
    private lateinit var binding: FragmentEditCategoryBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = categoryId?.let { categoryListViewModel.get(it)!! } ?:
                CategoryViewModel(requireActivity().application, null, order)
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditCategoryBinding.inflate(inflater, edit_sound_category_fragment, false)
        binding.viewModel = viewModel
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
                    viewModel.setName(catName)
                    viewModel.save()
                }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            view.select_colour_button.setOnClickListener { onSelectColourClick() }
            create()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.backgroundColor.observe(viewLifecycleOwner, { backgroundColor.set(it) })
        binding.backgroundColor = backgroundColor
    }

    private fun onSelectColourClick() {
        ColorPicker(requireActivity()).apply {
            setColors(colorHelper.colorStrings)
            setRoundColorButton(true)
            setTitle(getString(R.string.select_background_colour))
            setOnChooseColorListener(object : ColorPicker.OnChooseColorListener {
                override fun onChooseColor(position: Int, color: Int) { viewModel.setNewBackgroundColor(color) }
                override fun onCancel() {}
            })
            show()
        }
    }

    companion object {
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_ORDER = "order"

        @JvmStatic
        fun newInstance(categoryId: Int?, order: Int?): EditCategoryDialogFragment {
            return EditCategoryDialogFragment().apply {
                arguments = Bundle().apply {
                    if (categoryId != null) putInt(ARG_CATEGORY_ID, categoryId)
                    if (order != null) putInt(ARG_ORDER, order)
                }
            }
        }
    }
}