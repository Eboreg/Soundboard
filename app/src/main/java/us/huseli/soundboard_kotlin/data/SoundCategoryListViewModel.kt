package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SoundCategoryListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundCategoryRepository.getInstance(application)
    val categories: LiveData<List<SoundCategory>> by lazy { repository.categories }

    fun save(category: SoundCategory) = viewModelScope.launch {
        when(category.id) {
            null -> repository.insert(category)
            else -> repository.update(category)
        }
    }

    fun get(id: Int): SoundCategory? = repository.get(id)
}