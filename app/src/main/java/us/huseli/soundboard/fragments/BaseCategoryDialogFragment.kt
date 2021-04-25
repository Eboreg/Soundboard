package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentEditCategoryBinding
import us.huseli.soundboard.helpers.ColorHelper
import us.huseli.soundboard.viewmodels.BaseCategoryEditViewModel
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseCategoryDialogFragment : DialogFragment(), ColorPickerDialogListener {
    private val dialogId by lazy { requireArguments().getInt(ARG_DIALOG_ID) }

    protected lateinit var binding: FragmentEditCategoryBinding

    protected abstract val viewModel: BaseCategoryEditViewModel
    protected abstract val title: Int

    @Inject
    lateinit var colorHelper: ColorHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        /**
         * "This method will be called after onCreate() and immediately before onCreateView()"
         */
        binding = FragmentEditCategoryBinding.inflate(layoutInflater)

        binding.viewModel = viewModel
        binding.selectColorButton.setOnClickListener { onSelectColourClick() }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .create()

        // Custom listener to avoid automatic dismissal!
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onPositiveButtonClick() }
        }
        return dialog
    }

    protected open fun onPositiveButtonClick() {
        val catName = binding.categoryName.text.toString().trim()
        if (catName.isEmpty())
            Snackbar.make(binding.root, R.string.name_cannot_be_empty, Snackbar.LENGTH_SHORT).show()
        else {
            viewModel.setName(catName)
            save()
        }
    }

    protected open fun save() {
        viewModel.save()
        dismiss()
    }

/*
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            viewModel.let { viewModel ->
                state.getString(ARG_NAME)?.let { viewModel.name = it }
                viewModel.setBackgroundColor(state.getInt(ARG_BACKGROUND_COLOR))
            }
        }
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.let { outState.putString(ARG_NAME, it.categoryName.text.toString()) }
        viewModel.getBackgroundColor()?.let { backgroundColor -> outState.putInt(ARG_BACKGROUND_COLOR, backgroundColor) }
        super.onSaveInstanceState(outState)
    }
*/

    /**
     * I don't quite get why these following overrides are necessary, but the view binding of backgroundColor
     * won't work without them D-:
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /**
         * This has to be done here, otherwise: "Can't access the Fragment View's LifecycleOwner
         * when getView() is null i.e., before onCreateView() or after onDestroyView()"
         */
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
        const val ARG_DIALOG_ID = "dialogId"  // for ColorPickerDialog

        // For restoring instance state on recreate
        // const val ARG_NAME = "name"
        // const val ARG_BACKGROUND_COLOR = "backgroundColor"

        const val LOG_TAG = "BCategoryDialogFragment"
    }
}