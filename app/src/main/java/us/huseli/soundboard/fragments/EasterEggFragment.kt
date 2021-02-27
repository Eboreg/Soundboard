package us.huseli.soundboard.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentEasterEggBinding

class EasterEggFragment : DialogFragment() {
    private var binding: FragmentEasterEggBinding? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private fun play(resourceId: Int) = scope.launch { MediaPlayer.create(requireContext(), resourceId).start() }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEasterEggBinding.inflate(inflater)

        return MaterialAlertDialogBuilder(requireContext(),
            R.style.Soundboard_Theme_MaterialAlertDialog_EqualButtons).run {
            setTitle("HALLON!!!")
            setView(binding?.root)
            setPositiveButton("Hockeyklubba") { _, _ -> play(R.raw.hallon3) }
            setNegativeButton("Lugna puckar") { _, _ -> play(R.raw.hallon2) }
            create().also { play(R.raw.hallon1) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}