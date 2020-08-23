package us.huseli.soundboard_kotlin.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel

class DeleteSoundFragment : DialogFragment() {
    private val soundListViewModel by activityViewModels<SoundListViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val soundId = requireArguments().getInt(ARG_ID)
        val soundName = requireArguments().getString(ARG_NAME)!!

        return AlertDialog.Builder(requireContext()).run {
            setTitle(R.string.delete_sound)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            setMessage(resources.getString(R.string.delete_sound) + " $soundName?")
            setPositiveButton(R.string.ok) { _, _ -> soundListViewModel.delete(soundId) }
            create()
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_NAME = "name"

        @JvmStatic
        fun newInstance(soundId: Int, soundName: String): DeleteSoundFragment {
            return DeleteSoundFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, soundId)
                    putString(ARG_NAME, soundName)
                }
            }
        }
    }
}