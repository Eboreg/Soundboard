package us.huseli.soundboard_kotlin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.fragment_edit_sound.*
import kotlinx.android.synthetic.main.fragment_edit_sound.view.*
import us.huseli.soundboard_kotlin.data.SoundCategory
import us.huseli.soundboard_kotlin.data.SoundCategoryListViewModel
import us.huseli.soundboard_kotlin.data.SoundViewModel
import us.huseli.soundboard_kotlin.helpers.EditSoundInterface

open class EditSoundFragment : DialogFragment() {
    private lateinit var viewModel: SoundViewModel
    private val categoryListViewModel by viewModels<SoundCategoryListViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_sound, edit_sound_fragment, false).apply {
            sound_name.setText(viewModel.sound.name)
            sound_name.requestFocus()
            volume.progress = viewModel.volume
        }
        return AlertDialog.Builder(requireContext()).run {
            when (viewModel.sound.id) {
                null -> setTitle(R.string.add)
                else -> setTitle(R.string.edit_sound)
            }
            setView(view)
            setPositiveButton(R.string.save) { _, _ ->
                val soundName = view.sound_name.text.toString().trim()
                if (soundName.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val listener = requireActivity() as EditSoundInterface
                    viewModel.volume = view.volume.progress
                    viewModel.sound.name = soundName
                    viewModel.category = view.category.selectedItem as SoundCategory
                    listener.onSoundDialogSave(viewModel)
                    dismiss()
                }
            }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            categoryListViewModel.categories.value?.let {
                view.category.adapter = ArrayAdapter<SoundCategory>(context, R.layout.support_simple_spinner_dropdown_item, it)
                view.category.setSelection(it.indexOf(viewModel.category))
            }
            create()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(soundViewModel: SoundViewModel) =
                EditSoundFragment().also {
                    it.viewModel = soundViewModel
                }
    }
}