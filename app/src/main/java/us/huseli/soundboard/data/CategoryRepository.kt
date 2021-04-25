package us.huseli.soundboard.data

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val categoryDao: CategoryDao) {
    val categories = categoryDao.listLive()

    // Used by CategoryListViewModel.delete()
    fun delete(id: Int) = categoryDao.delete(id)

    fun getUsedColors() = categoryDao.getUsedColors()

    fun insert(category: Category) = categoryDao.insert(category)

    fun setCollapsed(categoryId: Int, value: Boolean) = categoryDao.updateCollapsed(categoryId, if (value) 1 else 0)

    fun switch(oldPos: Int, newPos: Int) {
        /** Switches places between two categories, updates .order, saves */
        val categoryIds = categoryDao.listIds().toMutableList()
        Collections.swap(categoryIds, oldPos, newPos)
        categoryDao.sort(categoryIds)
    }

    fun update(category: Category?, name: String?, backgroundColor: Int?) {
        if (category?.id != null) categoryDao.update(category.id, name, backgroundColor)
    }
}