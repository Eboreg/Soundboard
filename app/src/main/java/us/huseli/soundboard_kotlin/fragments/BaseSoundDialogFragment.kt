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
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

abstract class BaseSoundDialogFragment : DialogFragment() {
    // private lateinit var categoryViewModels: List<CategoryViewModel>
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    internal lateinit var binding: FragmentEditSoundBinding

    internal abstract var viewModel: SoundViewModel
    internal abstract val title: Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEditSoundBinding.inflate(inflater, edit_sound_fragment, false)
        binding.viewModel = viewModel
        binding.categoryListViewModel = categoryListViewModel
        binding.lifecycleOwner = requireActivity()
        val view = binding.root

        return AlertDialog.Builder(requireContext()).run {
            setTitle(title)
            setView(binding.root)
            setPositiveButton(R.string.save) { _, _ ->
                val soundName = view.sound_name.text.toString().trim()
                if (soundName.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.setVolume(view.volume.progress)
                    viewModel.setName(soundName)
                    viewModel.setCategoryId((view.category.selectedItem as CategoryViewModel).id.value!!)
                    viewModel.save()
                    dismiss()
                }
            }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            // view.category.adapter = ArrayAdapter<CategoryViewModel>(context, R.layout.support_simple_spinner_dropdown_item)
            // view.category.post { Runnable { view.category.setSelection(categoryViewModels.map { it.id.value }.indexOf(viewModel.categoryId.value)) } }
            create()
        }
    }
}
