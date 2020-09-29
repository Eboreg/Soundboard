package us.huseli.soundboard_kotlin.viewmodels

import android.graphics.Color
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.*

class CategoryViewModel : ViewModel() {
    private val database = SoundboardDatabase.getInstance(GlobalApplication.application)
    private val repository = CategoryRepository(database.categoryDao())
    private val soundRepository = SoundRepository(database.soundDao())

    private val _category = MutableLiveData<Category?>(null)
    private val _collapsed = MutableLiveData(false)

    val name = _category.map { it?.name }

    val backgroundColor = _category.map { it?.backgroundColor ?: Color.DKGRAY }

    val textColor = backgroundColor.map { bgc -> GlobalApplication.colorHelper.getTextColorForBackgroundColor(bgc) }

    val collapsed: LiveData<Boolean>
        get() = _collapsed

    val sounds = _category.switchMap { soundRepository.getByCategory(it?.id) }

    fun setCategory(category: Category) {
        _category.value = category
        _collapsed.value = category.collapsed
    }

    fun toggleCollapsed() {
        val newValue = !_collapsed.value!!
        _collapsed.value = newValue
        _category.value?.id?.let { categoryId ->
            viewModelScope.launch(Dispatchers.IO) { repository.setCollapsed(categoryId, newValue) }
        }
    }

    fun updateSoundOrder(sounds: List<Sound>) = viewModelScope.launch(Dispatchers.IO) {
        soundRepository.update(sounds)
    }

    override fun toString() = _category.value?.name ?: ""
}