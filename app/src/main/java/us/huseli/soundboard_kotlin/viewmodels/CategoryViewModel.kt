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
    private var categoryId: Int? = null

    private val _category = MutableLiveData<Category?>(null)
    private val _collapsed = MutableLiveData(false)

    val name = _category.map { it?.name }
    val backgroundColor = _category.map { it?.backgroundColor ?: Color.DKGRAY }
    val textColor = backgroundColor.map { bgc -> GlobalApplication.colorHelper.getTextColorForBackgroundColor(bgc) }
    val sounds = _category.switchMap { soundRepository.getByCategory(it?.id) }

    val collapsed: LiveData<Boolean>
        get() = _collapsed

    fun setCategory(category: Category) {
        categoryId = category.id
        _category.value = category
        _collapsed.value = category.collapsed
    }

    private fun setCollapsed(value: Boolean) {
        if (_collapsed.value != value) {
            _collapsed.value = value
            _category.value?.id?.let { categoryId ->
                viewModelScope.launch(Dispatchers.IO) { repository.setCollapsed(categoryId, value) }
            }
        }
    }

    fun toggleCollapsed() {
        val newValue = _collapsed.value?.let { !it } ?: true
        setCollapsed(newValue)
    }

    fun expand() = setCollapsed(false)

    fun collapse() = setCollapsed(true)

    fun updateSounds(sounds: List<Sound>) = viewModelScope.launch(Dispatchers.IO) {
        sounds.forEachIndexed { index, sound ->
            sound.order = index
            sound.categoryId = categoryId
        }
        soundRepository.update(sounds)
    }

    fun insertSound(soundId: Int, toPosition: Int, sounds: List<Sound>) = viewModelScope.launch(Dispatchers.IO) {
        /**
         * Fetch sound with id=soundId, insert it at position, run updateSounds()
         */
        soundRepository.get(soundId)?.let { sound ->
            val soundsMutable = sounds.toMutableList()
            val fromPosition = sounds.indexOf(sound)
            soundsMutable.remove(sound)
            if (fromPosition > -1 && fromPosition < toPosition)
                soundsMutable.add(toPosition - 1, sound)
            else
                soundsMutable.add(toPosition, sound)
            updateSounds(soundsMutable)
        }
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "CategoryViewModel $hashCode <category=${_category.value}>"
    }
}