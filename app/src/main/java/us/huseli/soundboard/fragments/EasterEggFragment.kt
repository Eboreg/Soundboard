package us.huseli.soundboard.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import us.huseli.soundboard.R
import us.huseli.soundboard.databinding.FragmentEasterEggBinding

class EasterEggFragment : DialogFragment() {
    var binding: FragmentEasterEggBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val inflater = LayoutInflater.from(requireContext())
        binding = FragmentEasterEggBinding.inflate(inflater)

        return MaterialAlertDialogBuilder(requireContext(), R.style.Soundboard_Theme_MaterialAlertDialog_EqualButtons).run {
            setTitle("HALLON!!!")
            setView(binding?.root)
            setPositiveButton("Hockeyklubba") { _, _ -> MediaPlayer.create(requireContext(), R.raw.hallon3).start() }
            setNegativeButton("Lugna puckar") { _, _ -> MediaPlayer.create(requireContext(), R.raw.hallon2).start() }
            create().also { MediaPlayer.create(requireContext(), R.raw.hallon1).start() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}