package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories: LiveData<List<CategoryExtended>> = categoryDao.getAll()

    fun insert(category: Category) {
        // If sounds exist, set category.order to max order + 1; else 0
        val lastCat = categories.value?.maxBy { it.category.order }
        lastCat?.category?.order?.let {
            category.order = it + 1
        }
        categoryDao.insert(category)
    }

    fun get(id: Int): CategoryExtended? = categories.value?.find { it.category.id == id }

    fun update(category: Category) = categoryDao.update(category)

    fun delete(category: Category) = categoryDao.delete(category)
}