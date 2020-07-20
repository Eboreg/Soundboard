package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val soundRepository = SoundRepository(SoundDatabase.getInstance(application, viewModelScope).soundDao())

    lateinit var category: Category
    lateinit var sounds: LiveData<List<Sound>>

    companion object {
        fun getInstance(application: Application, category: Category): CategoryViewModel {
            return CategoryViewModel(application).apply {
                this.category = category
                category.id?.let { sounds = soundRepository.soundsByCategory(it) }
            }
        }
    }
}