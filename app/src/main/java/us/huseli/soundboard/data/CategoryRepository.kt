package us.huseli.soundboard.data

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val categoryDao: CategoryDao) {
    val categories = categoryDao.listLive()

    // Used by CategoryListViewModel.delete()
    fun delete(id: Int) = categoryDao.delete(id)

    fun get(categoryId: Int) = categoryDao.get(categoryId)

    fun getUsedColors() = categoryDao.getUsedColors()

    fun getUsedColorsLive() = categoryDao.getUsedColorsLive()

    fun insert(category: Category) = categoryDao.insert(category)

    fun setCollapsed(categoryId: Int, value: Boolean) = categoryDao.updateCollapsed(categoryId, if (value) 1 else 0)

    fun update(category: Category) = categoryDao.update(category)

    fun switch(oldPos: Int, newPos: Int) {
        categoryDao.list().toMutableList().also {
            Collections.swap(it, oldPos, newPos)
            categoryDao.sort(it)
        }
    }

    fun update(category: Category?, name: String?, backgroundColor: Int?) {
        if (category?.id != null) categoryDao.update(category.id, name, backgroundColor)
    }
}