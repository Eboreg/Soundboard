package us.huseli.soundboard.fragments

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import us.huseli.soundboard.R

class EasterEggFragment : DialogFragment() {
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private fun play(resourceId: Int) = scope.launch {
        MediaPlayer.create(requireContext(), resourceId).apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.Soundboard_Theme_MaterialAlertDialog_EqualButtons)
            .setTitle("HALLON!!!")
            .setMessage("Hur fan är det med dig, karl?")
            .setPositiveButton("Hockeyklubba") { _, _ -> play(R.raw.hallon3) }
            .setNegativeButton("Lugna puckar") { _, _ -> play(R.raw.hallon2) }
            .create().also { play(R.raw.hallon1) }
    }
}