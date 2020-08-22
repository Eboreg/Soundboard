package us.huseli.soundboard_kotlin.data

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories = categoryDao.getAll()

    fun insert(category: Category) = categoryDao.insert(category)

    fun update(category: Category) = categoryDao.update(category)

    fun delete(category: Category) = categoryDao.delete(category)
}