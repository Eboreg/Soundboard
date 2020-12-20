package us.huseli.soundboard.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.SoundboardDatabase

@Keep
class CategoryViewModel(val categoryId: Int) : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application)
    private val repository = CategoryRepository(database.categoryDao())
    private val _category = repository.get(categoryId)

    val name = _category.map { it?.name }
    val backgroundColor = _category.map { it?.backgroundColor }
    val textColor = backgroundColor.map { bgc ->
        if (bgc != null) GlobalApplication.application.getColorHelper().getTextColorForBackgroundColor(bgc) else null
    }
    val collapsed = _category.map { it?.collapsed ?: false }

    private fun setCollapsed(value: Boolean) {
        if (collapsed.value != value) {
            viewModelScope.launch(Dispatchers.IO) { repository.setCollapsed(categoryId, value) }
        }
    }

    fun toggleCollapsed() {
        val newValue = collapsed.value?.let { !it } ?: true
        setCollapsed(newValue)
    }

    fun expand() = setCollapsed(false)

    fun collapse() = setCollapsed(true)

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "CategoryViewModel $hashCode <category=${_category.value}>"
    }
}