package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.databinding.FragmentDebugBinding
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel

private const val ARG_SOUND_ID = "soundId"

class DebugFragment : Fragment() {
    private val soundListViewModel by activityViewModels<SoundListViewModel>()
    private var soundId: Int? = null
    private lateinit var binding: FragmentDebugBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            soundId = it.getInt(ARG_SOUND_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDebugBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = soundListViewModel.getSoundViewModel(soundId!!)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(soundId: Int) =
                DebugFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_SOUND_ID, soundId)
                    }
                }
    }
}