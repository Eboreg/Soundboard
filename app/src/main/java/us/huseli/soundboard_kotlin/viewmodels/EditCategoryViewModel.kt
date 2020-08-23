package us.huseli.soundboard_kotlin.viewmodels

import androidx.lifecycle.ViewModel
import us.huseli.soundboard_kotlin.data.Category

class EditCategoryViewModel(category: Category) : ViewModel() {
    val isNew = category.id == null
    var name = category.name
    var backgroundColor = category.backgroundColor
}