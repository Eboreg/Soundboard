package us.huseli.soundboard_kotlin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_edit_sound.*
import kotlinx.android.synthetic.main.fragment_edit_sound.view.*
import us.huseli.soundboard_kotlin.data.SoundViewModel

open class EditSoundFragment : DialogFragment() {
    private lateinit var soundViewModel: SoundViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_sound, edit_sound_fragment, false).apply {
            sound_name.setText(soundViewModel.name)
            sound_name.requestFocus()
            volume.progress = soundViewModel.volume
        }
        return AlertDialog.Builder(requireContext()).run {
            when (soundViewModel.id) {
                null -> setTitle(R.string.add_sound_title)
                else -> setTitle(R.string.edit_sound_title)
            }
            setView(view)
            setPositiveButton(R.string.sound_save_button) { _, _ ->
                val soundName = view.sound_name.text.toString().trim()
                if (soundName!!.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.sound_name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val listener = requireActivity() as EditSoundInterface
                    soundViewModel.volume = view.volume.progress
                    soundViewModel.name = soundName
                    listener.onSoundDialogSave(soundViewModel)
                    dismiss()
                }
            }
            setNegativeButton(R.string.sound_cancel_button) { _, _ -> dismiss() }
            create()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(soundViewModel: SoundViewModel) =
                EditSoundFragment().also {
                    it.soundViewModel = soundViewModel
                }
    }
}