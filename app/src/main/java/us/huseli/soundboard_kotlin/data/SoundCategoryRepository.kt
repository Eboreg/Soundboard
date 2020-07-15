package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.LiveData

class SoundCategoryRepository(application: Application) {
    private val soundCategoryDao = SoundDatabase.getInstance(application).soundCategoryDao()
    val categories: LiveData<List<SoundCategory>> = soundCategoryDao.getAll()

    fun insert(category: SoundCategory) {
        // If sounds exist, set category.order to max order + 1; else 0
        val lastCat = categories.value?.maxBy { it.order }
        lastCat?.order?.let {
            category.order = it + 1
        }
        soundCategoryDao.insert(category)
    }

    fun get(id: Int): SoundCategory? = categories.value?.find { it.id == id }

    fun update(category: SoundCategory) = soundCategoryDao.update(category)

    companion object {
        @Volatile private var instance: SoundCategoryRepository? = null

        fun getInstance(application: Application): SoundCategoryRepository {
            return instance ?: synchronized(this) {
                instance ?: SoundCategoryRepository(application).also { instance = it }
            }
        }
    }
}