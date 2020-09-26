package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.viewmodels.SoundEditMultipleViewModel

class EditMultipleSoundDialogFragment : BaseEditMultipleSoundDialogFragment<SoundEditMultipleViewModel>() {
    override val viewModel by activityViewModels<SoundEditMultipleViewModel>()
    override var title = R.string.edit_sounds

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryListViewModel.categoriesWithEmpty.observe(viewLifecycleOwner, {
            binding.category.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, it)
        })
    }
}