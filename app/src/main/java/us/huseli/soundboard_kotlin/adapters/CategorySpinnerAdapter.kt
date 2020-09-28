package us.huseli.soundboard_kotlin.adapters

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.data.Category

class CategorySpinnerAdapter(context: Context, objects: List<Category>) :
        ArrayAdapter<Category>(context, R.layout.category_spinner_dropdown_item, R.id.category_spinner_item_text, objects) {

    private fun setItemColor(view: View, position: Int) {
        getItem(position)?.let { category ->
            val drawable = view.findViewById<ImageView>(R.id.category_spinner_item_color)?.drawable
            if (drawable is GradientDrawable)
                drawable.setColor(category.backgroundColor)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getView(position, convertView, parent).apply { setItemColor(this, position) }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getDropDownView(position, convertView, parent).apply { setItemColor(this, position) }
}