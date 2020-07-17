package us.huseli.soundboard_kotlin

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_edit_category.*
import kotlinx.android.synthetic.main.fragment_edit_category.view.*
import petrov.kristiyan.colorpicker.ColorPicker
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.helpers.EditSoundCategoryInterface

class EditCategoryFragment : DialogFragment() {
    private var category: Category = Category()
    private lateinit var currentColourDrawable: GradientDrawable

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_edit_category, edit_sound_category_fragment, false)
        return AlertDialog.Builder(requireContext()).run {
            when (category.id) {
                null -> setTitle(R.string.add_category)
                else -> setTitle(R.string.edit_category)
            }
            setView(view)
            setPositiveButton(R.string.save) { _, _ ->
                val catName = view.category_name.text.toString().trim()
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
            view.select_colour_button.setOnClickListener { onSelectColourClick() }
            currentColourDrawable = view.current_colour.drawable as GradientDrawable
            currentColourDrawable.setColor(category.backgroundColor)
            create()
        }
    }

    private fun onSelectColourClick() {
        ColorPicker(requireActivity()).apply {
            setColors(*COLOURS)
            setRoundColorButton(true)
            setTitle(getString(R.string.select_background_colour))
            setOnChooseColorListener(object : ColorPicker.OnChooseColorListener {
                override fun onChooseColor(position: Int, color: Int) {
                    currentColourDrawable.setColor(color)
                    category.backgroundColor = color
                    if (COLOURS_BLACK_BG.indexOf(color) > -1)
                        category.textColor = Color.BLACK
                    else
                        category.textColor = Color.WHITE
                }
                override fun onCancel() {}
            })
            show()
        }
    }

    companion object {
        val COLOURS_WHITE_BG = intArrayOf(Color.BLACK, Color.GRAY, Color.DKGRAY, Color.BLUE, Color.RED, Color.MAGENTA)
        val COLOURS_BLACK_BG = intArrayOf(Color.CYAN, Color.GREEN, Color.WHITE, Color.YELLOW)
        val COLOURS = COLOURS_BLACK_BG + COLOURS_WHITE_BG

        @JvmStatic
        fun newInstance(category: Category) = EditCategoryFragment().apply { this.category = category }
    }
}