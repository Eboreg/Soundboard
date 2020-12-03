package us.huseli.soundboard.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentEditCategoryBinding
import us.huseli.soundboard.viewmodels.BaseCategoryEditViewModel

abstract class BaseCategoryDialogFragment : DialogFragment(), ColorPickerDialogListener {
    private val dialogId by lazy { requireArguments().getInt(ARG_DIALOG_ID) }
    private var binding: FragmentEditCategoryBinding? = null

    internal abstract var viewModel: BaseCategoryEditViewModel?
    internal abstract val title: Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditCategoryBinding.inflate(inflater)
        // this.binding = binding
        binding?.viewModel = viewModel
        binding?.selectColorButton?.setOnClickListener { onSelectColourClick() }

        val dialog = AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(binding?.root)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
                .create()

        // Custom listener to avoid automatic dismissal!
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val catName = binding?.categoryName?.text.toString().trim()
                if (catName.isEmpty())
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                else {
                    viewModel?.apply {
                        setName(catName)
                        save()
                        dismiss()
                    } ?: run {
                        Log.e(LOG_TAG, "setPositiveButton: viewModel is null")
                    }
                }
            }
        }

        return dialog
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            viewModel?.let { viewModel ->
                state.getString(ARG_NAME)?.let { viewModel.setName(it) }
                viewModel.setBackgroundColor(state.getInt(ARG_BACKGROUND_COLOR))
            }
        }
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.let { outState.putString(ARG_NAME, it.categoryName.text.toString()) }
        viewModel?.backgroundColor?.value?.let { outState.putInt(ARG_BACKGROUND_COLOR, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // binding = FragmentEditCategoryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This has to be done here, otherwise: "Can't access the Fragment View's LifecycleOwner
        // when getView() is null i.e., before onCreateView() or after onDestroyView()"
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
        } ?: run {
            Log.e(LOG_TAG, "onViewCreated: binding is null")
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        viewModel?.apply {
            setBackgroundColor(color)
        } ?: run {
            Log.e(LOG_TAG, "onColorSelected: viewModel is null")
        }
    }

    override fun onDialogDismissed(dialogId: Int) = Unit

    private fun onSelectColourClick() {
        ColorPickerDialog.newBuilder().apply {
            setPresets(GlobalApplication.application.getColorHelper().colors.toIntArray())
            setDialogTitle(R.string.select_background_colour)
            viewModel?.backgroundColor?.value?.let { setColor(it) }
            setDialogId(dialogId)
            show(requireActivity())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_DIALOG_ID = "dialogId"  // for ColorPickerDialog

        // For restoring instance state on recreate
        const val ARG_NAME = "name"
        const val ARG_BACKGROUND_COLOR = "backgroundColor"
        const val LOG_TAG = "BCategoryDialogFragment"
    }
}