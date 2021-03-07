package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import us.huseli.soundboard.databinding.FragmentHelpBinding

class HelpFragment : Fragment(), View.OnClickListener {
    private var binding: FragmentHelpBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentHelpBinding.inflate(inflater, container, false)
        binding?.cancel?.setOnClickListener(this)
        return binding?.root
    }

    override fun onClick(view: View?) {
        if (view == binding?.cancel) {
            parentFragmentManager.popBackStackImmediate()
        }
    }
}