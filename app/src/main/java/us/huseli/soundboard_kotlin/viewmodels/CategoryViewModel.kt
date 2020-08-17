package us.huseli.soundboard_kotlin.viewmodels

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.CategoryExtended
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundDatabase

class CategoryViewModel(application: Application, category: CategoryExtended?) : AndroidViewModel(application) {
    /** Private fields */
    private val repository = CategoryRepository(SoundDatabase.getInstance(application, viewModelScope).categoryDao())
    private val _categoryWithSounds = category ?: CategoryExtended("", Color.DKGRAY, Color.WHITE, 0)

    /** Model fields */
    val id: Int? = _categoryWithSounds.category.id

    val name = liveData { emit(_categoryWithSounds.category.name) }
    fun setName(value: String) {
        if (value.trim().isNotEmpty()) _categoryWithSounds.category.name = value.trim()
    }

    // This particular xml attribute doesn't seem to work so well with data binding?
    private val _backgroundColor = MutableLiveData<Int>(_categoryWithSounds.category.backgroundColor)
    val backgroundColor: LiveData<Int>
        get() = _backgroundColor
    private fun setBackgroundColor(value: Int) {
        _backgroundColor.value = value
        val textColor = if (COLOURS_BLACK_BG.indexOf(value) > -1) Color.BLACK else Color.WHITE
        _categoryWithSounds.category.backgroundColor = value
        _categoryWithSounds.category.textColor = textColor
    }

    val textColor = liveData { emit(_categoryWithSounds.category.textColor) }

    // We are not observing this ATM, just reading it once from DeleteCategoryFragment
    val soundCount = _categoryWithSounds.soundCount

    /** Derived fields */
    val soundListViewModel: SoundListViewModel = id?.let { id -> SoundListViewModel(id) } ?: SoundListViewModel()

    private val _newBackgroundColor = MutableLiveData<Int>(_categoryWithSounds.category.backgroundColor)
    val newBackgroundColor: LiveData<Int>
        get() = _newBackgroundColor
    fun setNewBackgroundColor(value: Int) { _newBackgroundColor.value = value }

    /** Methods */
    override fun toString(): String {
        return name.value ?: "[no name]"
    }

    fun save() {
        _newBackgroundColor.value?.let { setBackgroundColor(it) }
        viewModelScope.launch(Dispatchers.IO) {
            when(_categoryWithSounds.category.id) {
                null -> repository.insert(_categoryWithSounds.category)
                else -> repository.update(_categoryWithSounds.category)
            }
        }
    }

    fun delete() = viewModelScope.launch(Dispatchers.IO) { repository.delete(_categoryWithSounds.category) }

    companion object {
        private val COLOURS_WHITE_BG = intArrayOf(Color.BLACK, Color.GRAY, Color.DKGRAY, Color.BLUE, Color.RED, Color.MAGENTA)
        val COLOURS_BLACK_BG = intArrayOf(Color.CYAN, Color.GREEN, Color.WHITE, Color.YELLOW)
        val COLOURS = COLOURS_BLACK_BG + COLOURS_WHITE_BG
    }
}