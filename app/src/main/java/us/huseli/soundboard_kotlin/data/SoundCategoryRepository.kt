package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData

class SoundCategoryRepository(private val soundCategoryDao: SoundCategoryDao) {
    val categories: LiveData<List<SoundCategory>> = soundCategoryDao.getAll()

    suspend fun insert(category: SoundCategory) {
        // If sounds exist, set category.order to max order + 1; else 0
        val lastCat = categories.value?.maxBy { it.order }
        lastCat?.order?.let {
            category.order = it + 1
        }
        soundCategoryDao.insert(category)
    }

    fun get(id: Int): SoundCategory? = categories.value?.find { it.id == id }

    suspend fun update(category: SoundCategory) = soundCategoryDao.update(category)
}