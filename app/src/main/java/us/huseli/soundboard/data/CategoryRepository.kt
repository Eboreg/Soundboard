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

    fun insert(category: Category) = categoryDao.insert(category)

    fun list() = categoryDao.list()

    /** Update/add categories, delete the rest */
    fun reset(categories: List<Category>) = categoryDao.reset(categories)

    fun setCollapsed(categoryId: Int, value: Boolean) = categoryDao.setCollapsed(categoryId, if (value) 1 else 0)

    fun sort(categories: List<Category>) = categoryDao.sort(categories)

    fun update(category: Category) = categoryDao.update(category)

    fun switch(oldPos: Int, newPos: Int) {
        list().toMutableList().also {
            Collections.swap(it, oldPos, newPos)
            sort(it)
        }
    }
}