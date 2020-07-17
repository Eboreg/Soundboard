package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories: LiveData<List<Category>> = categoryDao.getAll()

    suspend fun insert(category: Category) {
        // If sounds exist, set category.order to max order + 1; else 0
        val lastCat = categories.value?.maxBy { it.order }
        lastCat?.order?.let {
            category.order = it + 1
        }
        categoryDao.insert(category)
    }

    fun get(id: Int): Category? = categories.value?.find { it.id == id }

    suspend fun update(category: Category) = categoryDao.update(category)
}