package us.huseli.soundboard_kotlin.data

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories = categoryDao.getAll()

    val categoryWithSounds = categoryDao.getAllWithSounds()

    fun get(categoryId: Int) = categoryDao.get(categoryId)

    fun insert(category: Category) = categoryDao.insert(category)
    fun insert(category: CategoryWithSounds) = categoryDao.insert(category.toCategory())

    fun update(category: Category) = categoryDao.update(category)
    fun update(category: CategoryWithSounds) = categoryDao.update(category.toCategory())
    fun update(categories: List<CategoryWithSounds>) = categoryDao.update(categories)

    fun updateOrder(categories: List<CategoryWithSounds>) = categoryDao.updateOrder(categories.map { it.toCategory() })

    fun delete(category: Category) = categoryDao.delete(category)

    fun delete(id: Int) = categoryDao.delete(id)

    fun delete(category: CategoryWithSounds) = categoryDao.delete(category)
}