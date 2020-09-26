package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundDeleteViewModel

class DeleteSoundFragment : BaseSoundDialogFragment() {
    override var title = R.string.delete_sound
    override val positiveButtonText = R.string.delete
    private var soundIds = emptyList<Int>()

    override fun onPositiveButtonClick() {
        SoundDeleteViewModel().delete(soundIds)
        super.onPositiveButtonClick()
    }

    override fun configureDialog(builder: AlertDialog.Builder) {
        requireArguments().getIntegerArrayList(ARG_IDS)?.let { soundIds = it }

        builder.apply {
            if (soundIds.size > 1) {
                title = R.string.delete_sounds
                setMessage(getString(R.string.delete_sounds_count, soundIds.size))
            } else {
                val soundName = requireArguments().getString(ARG_NAME) ?: ""
                setMessage(resources.getString(R.string.delete_sound_name, soundName))
            }
        }
        super.configureDialog(builder)
    }

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