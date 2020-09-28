package us.huseli.soundboard_kotlin.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategorySpinnerAdapter(context: Context, objects: List<Category>, private val colorHelper: ColorHelper) :
        ArrayAdapter<Category>(context, android.R.layout.simple_spinner_dropdown_item, objects) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            getItem(position)?.let { category ->
                setBackgroundColor(category.backgroundColor)
                if (this is TextView) this.setTextColor(colorHelper.getTextColorForBackgroundColor(category.backgroundColor))
            }
        }
    }
}