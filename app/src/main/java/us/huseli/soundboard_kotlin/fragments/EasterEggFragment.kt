package us.huseli.soundboard_kotlin.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.databinding.FragmentEasterEggBinding

class EasterEggFragment : DialogFragment() {
    var binding: FragmentEasterEggBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        // binding = FragmentEasterEggBinding.inflate(inflater, easter_egg_fragment, false)
        binding = FragmentEasterEggBinding.inflate(inflater)

        return AlertDialog.Builder(requireContext()).run {
            setTitle(getString(R.string.easter_egg_tite))
            setView(binding?.root)
            setPositiveButton(getString(R.string.easter_egg_positive_button)) { _, _ -> MediaPlayer.create(requireContext(), R.raw.hallon3).start() }
            setNegativeButton(getString(R.string.easter_egg_negative_button)) { _, _ -> MediaPlayer.create(requireContext(), R.raw.hallon2).start() }
            create().also { MediaPlayer.create(requireContext(), R.raw.hallon1).start() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}