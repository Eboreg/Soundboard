package us.huseli.soundboard.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.CategoryRepository
import us.huseli.soundboard.helpers.ColorHelper
import javax.inject.Inject

@HiltViewModel
class CategoryAddViewModel @Inject constructor(
    private val repository: CategoryRepository, private val colorHelper: ColorHelper
) : BaseCategoryEditViewModel() {
    private val _backgroundColor = MutableLiveData<Int>()
    private val _name = MutableLiveData("")

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _backgroundColor.postValue(colorHelper.randomColor(repository.getUsedColors()))
        }
    }

    override val backgroundColor: LiveData<Int>
        get() = _backgroundColor
    override val name: LiveData<String?>
        get() = _name

    override fun setName(value: String) {
        _name.value = value
    }

    override fun setBackgroundColor(value: Int) {
        _backgroundColor.value = value
    }

    override fun save() = viewModelScope.launch(Dispatchers.IO) {
        /**
         * Used by AddCategoryDialogFrament & EditCategoryDialogFrament when saving.
         * Background colour was initialized from the beginning, so we know it's set.
         * And we check for name in BaseCategoryDialogFragment (and set it at init).
         * But still, we want to make sure.
         */
        val name = _name.value
        val backgroundColor = _backgroundColor.value
        if (name != null && backgroundColor != null) {
            val category = Category(name, backgroundColor)
            repository.insert(category)
        } else
            Log.e(LOG_TAG, "save(): name ($name) or backgroundColor ($backgroundColor) is null")
    }


    companion object {
        const val LOG_TAG = "CategoryAddViewModel"
    }
}