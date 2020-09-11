package us.huseli.soundboard_kotlin.data

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories = categoryDao.getAll()

    // Used by CategoryEditViewModel
    fun get(categoryId: Int) = categoryDao.get(categoryId)

    fun insert(category: Category) = categoryDao.insert(category)

    fun update(category: Category) = categoryDao.update(category)

    // Used by CategoryListViewModel.updateOrder()
    fun updateOrder(categories: List<Category>) = categoryDao.updateOrder(categories)

    // Used by CategoryListViewModel.delete()
    fun delete(id: Int) = categoryDao.delete(id)

    // Save .order of all categories as set right now
    fun saveOrder(categories: List<Category>) = categoryDao.saveOrder(categories)
}