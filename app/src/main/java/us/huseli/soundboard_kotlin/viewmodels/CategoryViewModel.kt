package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryViewModel(private val category: Category) : ViewModel() {
    private val colorHelper = ColorHelper(GlobalApplication.application)

    /** Model fields */
    val id = category.id

    val order = category.order

    val name = category.name

    val backgroundColor = category.backgroundColor

    val textColor = colorHelper.getTextColorForBackgroundColor(backgroundColor)

    /** Methods */
    override fun toString() = category.name
}