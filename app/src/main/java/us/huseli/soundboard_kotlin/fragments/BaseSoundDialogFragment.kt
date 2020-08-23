package us.huseli.soundboard_kotlin.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_edit_sound.*
import kotlinx.android.synthetic.main.fragment_edit_sound.view.*
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.databinding.FragmentEditSoundBinding
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundEditViewModel

abstract class BaseSoundDialogFragment : DialogFragment() {
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    internal lateinit var binding: FragmentEditSoundBinding

    internal abstract var viewModel: SoundEditViewModel
    internal abstract val title: Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let { state ->
            state.getString(ARG_NAME)?.let { viewModel.name = it }
            viewModel.categoryIndex = state.getInt(ARG_CATEGORY_INDEX)
            viewModel.volume = state.getInt(ARG_VOLUME)
        }

        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditSoundBinding.inflate(inflater, edit_sound_fragment, false)
        binding.viewModel = viewModel
        binding.categoryListViewModel = categoryListViewModel
        binding.lifecycleOwner = requireActivity()

        return AlertDialog.Builder(requireContext()).run {
            setTitle(title)
            setView(binding.root)
            setPositiveButton(R.string.save) { _, _ ->
                val soundName = binding.soundName.text.toString().trim()
                if (soundName.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.volume = binding.volume.progress
                    viewModel.name = soundName
                    viewModel.categoryId = (binding.category.selectedItem as CategoryViewModel).id!!
                    viewModel.save()
                    dismiss()
                }
            }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            create()
        }
    }

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
