package us.huseli.soundboard.viewmodels

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.data.UndoRepository
import javax.inject.Inject

@HiltViewModel
class CategoryAddViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val undoRepository: UndoRepository
) : BaseCategoryEditViewModel() {

    override val backgroundColor = MediatorLiveData<Int>().apply {
        addSource(repository.randomColor.asLiveData()) { value = it }
        addSource(_backgroundColor) { value = it }
    }

    /**
     * Used by AddCategoryDialogFrament & EditCategoryDialogFrament when saving. Background colour was initialized
     * from the beginning, so we know it's set. And we check for name in BaseCategoryDialogFragment (and set it at
     * init). But still, we want to make sure.
     */
    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        val backgroundColor = backgroundColor.value
        val name = _name.value ?: ""
        if (name.isNotBlank() && backgroundColor != null) {
            val category = Category(name, backgroundColor)
            repository.insert(category)
            undoRepository.pushState()
        } else Log.e(LOG_TAG, "save(): name ($name) is blank or backgroundColor ($backgroundColor) is null")
    }


    companion object {
        const val LOG_TAG = "CategoryAddViewModel"
    }
}