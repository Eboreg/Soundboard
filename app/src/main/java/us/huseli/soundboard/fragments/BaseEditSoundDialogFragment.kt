package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import us.huseli.soundboard.R
import us.huseli.soundboard.adapters.CategorySpinnerAdapter
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.databinding.FragmentEditSoundBinding
import us.huseli.soundboard.viewmodels.BaseSoundEditViewModel
import us.huseli.soundboard.viewmodels.CategoryViewModel

abstract class BaseEditSoundDialogFragment<VM : BaseSoundEditViewModel> : BaseSoundDialogFragment() {
    private lateinit var binding: FragmentEditSoundBinding
    protected val categoryListViewModel by activityViewModels<CategoryViewModel>()
    protected abstract val viewModel: VM

    /** OPEN/ABSTRACT METHODS */
    protected open fun getCategories() = categoryListViewModel.categories

    protected open fun recreateFromSavedInstanceState(state: Bundle) {
        state.getString(ARG_NAME)?.let { viewModel.name = it }
        viewModel.categoryIndex = state.getInt(ARG_CATEGORY_INDEX)
        viewModel.volume = state.getInt(ARG_VOLUME)
    }

    abstract fun save(): Any?


    /** OVERRIDDEN OWN METHODS */
    override fun configureDialog(builder: MaterialAlertDialogBuilder) {
        super.configureDialog(builder)
        builder.setView(binding.root)
    }

    override fun onPositiveButtonClick() {
        val soundName = binding.soundName.text.toString().trim()
        if (soundName.isEmpty() && !viewModel.multiple) {
            Snackbar.make(binding.root, R.string.name_cannot_be_empty, Snackbar.LENGTH_SHORT).show()
        } else {
            viewModel.volume = binding.volume.progress
            viewModel.name = soundName
            (binding.category.selectedItem as Category).id?.let { viewModel.categoryId = it }
            soundViewModel.disableSelect()
            save()
            dismiss()
        }
    }


    /** OVERRIDDEN STANDARD METHODS */
    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditSoundBinding.inflate(inflater)
        binding.viewModel = viewModel
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        binding.root

    override fun onResume() {
        /**
         * Set pre-selected category.
         * For some reason, this will only work from this method.
         * https://stackoverflow.com/a/25315436
         */
        super.onResume()
        viewModel.categoryIndex?.let {
            binding.category.setSelection(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARG_NAME, binding.soundName.text.toString())
        outState.putInt(ARG_VOLUME, binding.volume.progress)
        outState.putInt(ARG_CATEGORY_INDEX, binding.category.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.soundName.isEnabled = !viewModel.multiple
        // This has to be done here, otherwise: "Can't access the Fragment View's LifecycleOwner
        // when getView() is null i.e., before onCreateView() or after onDestroyView()"
        binding.lifecycleOwner = viewLifecycleOwner
        getCategories().observe(viewLifecycleOwner) {
            binding.category.adapter = CategorySpinnerAdapter(requireContext(), it)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.let { recreateFromSavedInstanceState(it) }
        super.onViewStateRestored(savedInstanceState)
    }


    companion object {
        const val ARG_ID = "soundId"
        const val ARG_CATEGORY_INDEX = "categoryIndex"

        // For use in instance state
        const val ARG_NAME = "name"
        const val ARG_VOLUME = "volume"

        const val LOG_TAG = "BESoundDialogFragment"
    }
}
