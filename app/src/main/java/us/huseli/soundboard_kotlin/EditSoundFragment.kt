package us.huseli.soundboard_kotlin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_edit_sound.*
import kotlinx.android.synthetic.main.fragment_edit_sound.view.*

open class EditSoundFragment : DialogFragment() {
    private var soundId: Int? = null
    private var soundName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            soundId = it.getInt(SoundListFragment.ARG_SOUND_ID)
            soundName = it.getString(SoundListFragment.ARG_SOUND_NAME)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_sound, edit_sound_fragment, false).apply {
            sound_name.setText(soundName)
            sound_name.requestFocus()
        }
        return AlertDialog.Builder(requireContext()).run {
            when (soundId) {
                null -> setTitle(R.string.add_sound_title)
                else -> setTitle(R.string.edit_sound_title)
            }
            setView(view)
            setPositiveButton(R.string.sound_save_button) { _, _ ->
                soundName = view.sound_name.text.toString().trim()
                if (soundName!!.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.sound_name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val listener = requireActivity() as EditSoundInterface
                    listener.onSoundDialogSave(Bundle().apply {
                        soundId?.let { putInt(SoundListFragment.ARG_SOUND_ID, it) }
                        putString(SoundListFragment.ARG_SOUND_NAME, soundName)
                    })
                    dismiss()
                }
            }
            setNegativeButton(R.string.sound_cancel_button) { _, _ -> dismiss() }
            create()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(soundId: Int?, soundName: String) =
                EditSoundFragment().apply {
                    arguments = Bundle().apply {
                        if (soundId != null)
                            putInt(SoundListFragment.ARG_SOUND_ID, soundId)
                        putString(SoundListFragment.ARG_SOUND_NAME, soundName)
                    }
                }
    }
}