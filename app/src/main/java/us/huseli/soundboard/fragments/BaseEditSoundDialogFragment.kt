package us.huseli.soundboard.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.R
import us.huseli.soundboard.adapters.CategorySpinnerAdapter
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.databinding.FragmentEditSoundBinding
import us.huseli.soundboard.viewmodels.BaseSoundEditViewModel
import us.huseli.soundboard.viewmodels.CategoryViewModel

abstract class BaseEditSoundDialogFragment<VM : BaseSoundEditViewModel> : BaseSoundDialogFragment() {
    internal val categoryListViewModel by activityViewModels<CategoryViewModel>()
    internal open var binding: FragmentEditSoundBinding? = null
    internal open var multiple = false
    internal abstract val viewModel: VM

    /** OPEN METHODS */
    open fun getCategories() = categoryListViewModel.categories

    internal open fun recreateFromSavedInstanceState(state: Bundle) {
        state.getString(ARG_NAME)?.let { viewModel.setName(it) }
        viewModel.categoryIndex = state.getInt(ARG_CATEGORY_INDEX)
        viewModel.setVolume(state.getInt(ARG_VOLUME))
    }

    internal open fun save() {
        appViewModel.pushSoundUndoState(requireContext())
        viewModel.save(requireContext())
    }


    /** OVERRIDDEN OWN METHODS */
    override fun configureDialog(builder: MaterialAlertDialogBuilder) {
        super.configureDialog(builder)
        binding?.let { builder.setView(it.root) }
    }

    override fun onPositiveButtonClick() {
        binding?.let { binding ->
            val soundName = binding.soundName.text.toString().trim()
            if (soundName.isEmpty() && !multiple) {
                Snackbar.make(binding.root, R.string.name_cannot_be_empty, Snackbar.LENGTH_SHORT).show()
            } else {
                viewModel.setVolume(binding.volume.progress)
                viewModel.setName(soundName)
                (binding.category.selectedItem as Category).id?.let { viewModel.setCategoryId(it) }
                soundViewModel.disableSelect()
                save()
                dismiss()
            }
        } ?: run {
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "onPositiveButtonClick(): binding is null")
            dismiss()
        }
    }


    /** OVERRIDDEN STANDARD METHODS */
    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditSoundBinding.inflate(inflater).also {
            it.viewModel = viewModel
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = binding?.root

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        /**
         * Set pre-selected category.
         * For some reason, this will only work from this method.
         * https://stackoverflow.com/a/25315436
         */
        super.onResume()
        viewModel.categoryIndex?.let {
            binding?.category?.setSelection(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.let { binding ->
            outState.putString(ARG_NAME, binding.soundName.text.toString())
            outState.putInt(ARG_VOLUME, binding.volume.progress)
            outState.putInt(ARG_CATEGORY_INDEX, binding.category.selectedItemPosition)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        multiple = viewModel.multiple
        binding?.soundName?.isEnabled = !multiple
        // This has to be done here, otherwise: "Can't access the Fragment View's LifecycleOwner
        // when getView() is null i.e., before onCreateView() or after onDestroyView()"
        binding?.let { binding ->
            binding.lifecycleOwner = viewLifecycleOwner
            getCategories().observe(viewLifecycleOwner) { binding.category.adapter = CategorySpinnerAdapter(requireContext(), it) }
        } ?: run {
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "onViewCreated: binding is null")
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
