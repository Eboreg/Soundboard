package us.huseli.soundboard_kotlin.data

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories = categoryDao.getAll()

    fun get(categoryId: Int) = categoryDao.get(categoryId)

    fun insert(category: Category) = categoryDao.insert(category)

    fun update(category: Category) = categoryDao.update(category)

    fun updateOrder(categories: List<Category>) = categoryDao.updateOrder(categories)

    fun delete(id: Int) = categoryDao.delete(id)
}