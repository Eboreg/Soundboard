package us.huseli.soundboard_kotlin.viewmodels

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.data.CategoryExtended
import us.huseli.soundboard_kotlin.data.CategoryRepository
import us.huseli.soundboard_kotlin.data.SoundDatabase

class CategoryViewModel(application: Application, category: CategoryExtended?) : AndroidViewModel(application) {
    private val repository = CategoryRepository(SoundDatabase.getInstance(application, viewModelScope).categoryDao())
    private val _categoryWithSounds = category ?: CategoryExtended("", Color.DKGRAY, Color.WHITE, 0)

/*
    private val _id = MutableLiveData<Int?>()
    val id: LiveData<Int?>
        get() = _id
*/

    val id: Int?

    private val _name = MutableLiveData<String>()
    val name: LiveData<String>
        get() = _name

    private val _backgroundColor = MutableLiveData<Int>()
    val backgroundColor: LiveData<Int>
        get() = _backgroundColor

    private val _newBackgroundColor = MutableLiveData<Int>()
    val newBackgroundColor: LiveData<Int>
        get() = _newBackgroundColor

    private val _textColor = MutableLiveData<Int>()
    val textColor: LiveData<Int>
        get() = _textColor

    private val _order = MutableLiveData<Int>()
    val order: LiveData<Int>
        get() = _order

    private val _soundCount = MutableLiveData<Int>()
    val soundCount: LiveData<Int>
        get() = _soundCount

    val soundListViewModel: SoundListViewModel

    init {
        // TODO: Refactor this as in SoundViewModel
        // _id.value = _categoryWithSounds.category.id
        id = _categoryWithSounds.category.id
        _name.value = _categoryWithSounds.category.name
        _backgroundColor.value = _categoryWithSounds.category.backgroundColor
        _newBackgroundColor.value = _categoryWithSounds.category.backgroundColor
        _textColor.value = _categoryWithSounds.category.textColor
        _order.value = _categoryWithSounds.category.order
        _soundCount.value = _categoryWithSounds.soundCount
        soundListViewModel = id?.let { id -> SoundListViewModel(id) } ?: SoundListViewModel()
    }

    override fun toString(): String {
        return name.value ?: "[no name]"
    }

    fun setName(value: String) {
        if (value.trim().isNotEmpty()){
            _name.value = value.trim()
            _categoryWithSounds.category.name = value.trim()
        }
    }

    private fun setBackgroundColor(value: Int) {
        _backgroundColor.value = value
        val textColor = if (COLOURS_BLACK_BG.indexOf(value) > -1) Color.BLACK else Color.WHITE
        _categoryWithSounds.category.backgroundColor = value
        _categoryWithSounds.category.textColor = textColor
    }

    fun setNewBackgroundColor(value: Int) { _newBackgroundColor.value = value }

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