package us.huseli.soundboard_kotlin.viewmodels

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.CategoryExtended
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundDatabase
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class CategoryViewModel(application: Application, category: CategoryExtended?, order: Int?) : AndroidViewModel(application) {
    private val colorHelper = ColorHelper(application)

    /** Private fields */
    private val repository = CategoryRepository(SoundDatabase.getInstance(application, viewModelScope).categoryDao())
    private val _categoryWithSounds = category ?: CategoryExtended(colorHelper.colors.random(), order ?: 0)

    /** Model fields */
    val id = _categoryWithSounds.category.id

    val order = _categoryWithSounds.category.order

    val name = liveData { emit(_categoryWithSounds.category.name) }
    fun setName(value: String) {
        if (value.trim().isNotEmpty()) _categoryWithSounds.category.name = value.trim()
    }

    // This particular xml attribute doesn't seem to work so well with data binding?
    private val _backgroundColor = MutableLiveData(_categoryWithSounds.category.backgroundColor)
    val backgroundColor: LiveData<Int>
        get() = _backgroundColor

    private fun setBackgroundColor(value: Int) {
        _backgroundColor.value = value
        _categoryWithSounds.category.backgroundColor = value
        _categoryWithSounds.category.textColor = colorHelper.getTextColorForBackgroundColor(value)
    }

    val textColor = liveData { emit(_categoryWithSounds.category.textColor) }

    // We are not observing this ATM, just reading it once from DeleteCategoryFragment
    val soundCount = _categoryWithSounds.soundCount

    /** Derived fields */
    val soundListViewModel: SoundListViewModel = id?.let { id -> SoundListViewModel(id) } ?: SoundListViewModel()

    // Initiate it with model's backgroundcolor, but let it change independently, because we don't
    // want the "real" backgroundcolor to change until we eventually save
    private val _newBackgroundColor = MutableLiveData(_categoryWithSounds.category.backgroundColor)
    val newBackgroundColor: LiveData<Int>
        get() = _newBackgroundColor

    fun setNewBackgroundColor(value: Int) {
        _newBackgroundColor.value = value
    }

    /** Methods */
    override fun toString() = _categoryWithSounds.category.name

    fun save() {
        _newBackgroundColor.value?.let { setBackgroundColor(it) }
        viewModelScope.launch(Dispatchers.IO) {
            when (_categoryWithSounds.category.id) {
                null -> repository.insert(_categoryWithSounds.category)
                else -> repository.update(_categoryWithSounds.category)
            }
        }
    }

    fun delete() = viewModelScope.launch(Dispatchers.IO) { repository.delete(_categoryWithSounds.category) }
}