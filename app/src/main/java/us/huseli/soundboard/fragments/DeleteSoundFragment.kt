package us.huseli.soundboard.fragments

import android.app.Dialog
import android.os.Bundle
import us.huseli.soundboard.R
import us.huseli.soundboard.viewmodels.SoundDeleteViewModel

class DeleteSoundFragment : BaseSoundDialogFragment() {
    override val positiveButtonText = R.string.delete

    // private var soundIds = emptyList<Int>()
    private var soundIds: List<Int>? = null
    private var soundName: String? = null

    override fun getTitle() =
            resources.getQuantityString(R.plurals.delete_sound_title, soundIds?.size ?: 0)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        soundIds = requireArguments().getIntegerArrayList(ARG_IDS)
        soundName = requireArguments().getString(ARG_NAME)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onPositiveButtonClick() {
        SoundDeleteViewModel().delete(soundIds)
        super.onPositiveButtonClick()
    }

    override fun getMessage() =
            soundName?.let { getString(R.string.delete_sound_name, it) }
                    ?: getString(R.string.delete_sounds_count, soundIds?.size)

    companion object {
        const val ARG_NAME = "name"
        const val ARG_IDS = "ids"

        private fun makeBundle(soundIds: List<Int?>, soundName: String?) = Bundle().apply {
            putIntegerArrayList(ARG_IDS, ArrayList(soundIds))
            if (soundName != null) putString(ARG_NAME, soundName)
        }

        @JvmStatic
        fun newInstance(soundIds: List<Int?>): DeleteSoundFragment {
            return DeleteSoundFragment().apply {
                arguments = makeBundle(soundIds, null)
            }
        }

        @JvmStatic
        fun newInstance(soundId: Int, soundName: String): DeleteSoundFragment {
            return DeleteSoundFragment().apply {
                arguments = makeBundle(arrayListOf(soundId), soundName)
            }
        }
    }
}