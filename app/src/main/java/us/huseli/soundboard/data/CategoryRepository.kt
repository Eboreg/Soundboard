package us.huseli.soundboard.data

class CategoryRepository(private val categoryDao: CategoryDao) {
    val categories = categoryDao.getAll()

    // Used by CategoryEditViewModel
    fun get(categoryId: Int) = categoryDao.get(categoryId)

    fun insert(category: Category) = categoryDao.insert(category)

    fun update(category: Category) = categoryDao.update(category)

    fun getUsedColors() = categoryDao.getUsedColors()

    // Used by CategoryListViewModel.delete()
    fun delete(id: Int) = categoryDao.delete(id)

    // Save .order of all categories as set right now
    fun saveOrder(categories: List<Category>) = categoryDao.saveOrder(categories)

    fun setCollapsed(categoryId: Int, value: Boolean) = categoryDao.setCollapsed(categoryId, if (value) 1 else 0)
}