package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.adapters.SoundAdapter
import us.huseli.soundboard.databinding.FragmentSoundListBinding
import us.huseli.soundboard.viewmodels.CategoryViewModel2

@AndroidEntryPoint
class SoundListFragment : Fragment() {
    private val categoryViewModel by viewModels<CategoryViewModel2>()
    private lateinit var binding: FragmentSoundListBinding
    private var soundAdapter: SoundAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSoundListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundAdapter?.setLifecycleDestroyed()
        soundAdapter = null
    }
}