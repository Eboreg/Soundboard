package us.huseli.soundboard_kotlin

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_edit_sound_category.*
import kotlinx.android.synthetic.main.fragment_edit_sound_category.view.*
import us.huseli.soundboard_kotlin.data.SoundCategory
import us.huseli.soundboard_kotlin.helpers.EditSoundCategoryInterface

class EditSoundCategoryFragment : DialogFragment() {
    private var category: SoundCategory = SoundCategory()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_sound_category, edit_sound_category_fragment, false)
        return AlertDialog.Builder(requireContext()).run {
            when (category) {
                null -> setTitle(R.string.add_category)
                else -> setTitle(R.string.edit_category)
            }
            setView(view)
            setPositiveButton(R.string.save) { _, _ ->
                val catName = view.sound_category_name.text.toString().trim()
                if (catName.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val listener = requireActivity() as EditSoundCategoryInterface
                    category.name = catName
                    listener.onSoundCategoryDialogSave(category)
                    dismiss()
                }
            }
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            create()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(category: SoundCategory) = EditSoundCategoryFragment().apply { this.category = category }
    }
}