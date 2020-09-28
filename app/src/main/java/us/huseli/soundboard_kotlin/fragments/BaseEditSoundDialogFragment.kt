package us.huseli.soundboard_kotlin.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_edit_sound.*
import kotlinx.android.synthetic.main.fragment_edit_sound.view.*
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.CategorySpinnerAdapter
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.databinding.FragmentEditSoundBinding
import us.huseli.soundboard_kotlin.viewmodels.BaseSoundEditViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel

abstract class BaseEditSoundDialogFragment<VM: BaseSoundEditViewModel> : BaseSoundDialogFragment() {
    internal val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    internal open lateinit var binding: FragmentEditSoundBinding
    internal abstract val viewModel: VM

    open fun getCategories() = categoryListViewModel.categories

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditSoundBinding.inflate(inflater, edit_sound_fragment, false)
        binding.viewModel = viewModel

        return super.onCreateDialog(savedInstanceState)
    }

    override fun configureDialog(builder: AlertDialog.Builder) {
        super.configureDialog(builder)
        builder.setView(binding.root)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.let { recreateFromSavedInstanceState(it) }
        super.onViewStateRestored(savedInstanceState)
    }

    internal open fun recreateFromSavedInstanceState(state: Bundle) {
        state.getString(ARG_NAME)?.let { viewModel.setName(it) }
        viewModel.categoryIndex = state.getInt(ARG_CATEGORY_INDEX)
        viewModel.setVolume(state.getInt(ARG_VOLUME))
    }

    override fun onPositiveButtonClick() {
        val soundName = binding.soundName.text.toString().trim()
        if (soundName.isEmpty()) {
            Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setVolume(binding.volume.progress)
            viewModel.setName(soundName)
            viewModel.setCategoryId((binding.category.selectedItem as Category).id!!)
            appViewModel.disableSelect()
            save()
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This has to be done here, otherwise: "Can't access the Fragment View's LifecycleOwner
        // when getView() is null i.e., before onCreateView() or after onDestroyView()"
        binding.lifecycleOwner = viewLifecycleOwner
        getCategories().observe(viewLifecycleOwner, { binding.category.adapter = CategorySpinnerAdapter(requireContext(), it) })
    }

    internal open fun save() = viewModel.save()

    override fun onResume() {
        /**
         * Set pre-selected category.
         * For some reason, this will only work from this method.
         * https://stackoverflow.com/a/25315436
         */
        super.onResume()
        viewModel.categoryIndex?.let {
            binding.root.category?.setSelection(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARG_NAME, binding.soundName.text.toString())
        outState.putInt(ARG_VOLUME, binding.volume.progress)
        outState.putInt(ARG_CATEGORY_INDEX, binding.category.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val ARG_ID = "soundId"
        const val ARG_CATEGORY_INDEX = "categoryIndex"
        // For use in instance state
        const val ARG_NAME = "name"
        const val ARG_VOLUME = "volume"
    }
}
