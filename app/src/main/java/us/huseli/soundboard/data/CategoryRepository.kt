@file:Suppress("RedundantSuspendModifier")

package us.huseli.soundboard.data

import android.content.Context
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import us.huseli.soundboard.R
import us.huseli.soundboard.helpers.ColorHelper
import us.huseli.soundboard.helpers.Functions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val colorHelper: ColorHelper,
    private val undoRepository: UndoRepository,
) {
    val categories = categoryDao.list().asLiveData()
    val randomColor = categoryDao.getUsedColors().map { colorHelper.getRandomColor(it) }

    // Used by CategoryListViewModel.delete()
    suspend fun delete(id: Int) = categoryDao.delete(id)

    suspend fun getAutoImportCategory(context: Context): Category {
        if (categoryDao.list().firstOrNull()?.none { it.autoImportCategory } == true) {
            val category = Category(
                Functions.umlautify(context.getString(R.string.auto_imported_sounds)).toString(),
                randomColor.first(),
                true
            )
            insert(category)
            undoRepository.replaceCurrentState()
        }
        return categoryDao.list().first().first { it.autoImportCategory }
    }

    suspend fun insert(category: Category) {
        if (category.order == -1) {
            categoryDao.insert(Category(
                category.id,
                category.name,
                category.backgroundColor,
                categoryDao.getMaxOrder() + 1,
                category.collapsed,
                category.autoImportCategory
            ))
        }
        else categoryDao.insert(category)
    }

    suspend fun setCollapsed(id: Int, value: Boolean) = categoryDao.updateCollapsed(id, value)

    suspend fun swap(pos1: Int, pos2: Int) {
        /** Switches places between two categories, updates .order, saves */
        if (pos1 != pos2) {
            val ids = categoryDao.listIds()
            val id1 = ids[pos1]
            val id2 = ids[pos2]
            categoryDao.updateOrder(id1, pos2)
            categoryDao.updateOrder(id2, pos1)
        }
    }

    suspend fun update(id: Int?, name: String?, backgroundColor: Int?) {
        if (id != null) {
            if (name != null) categoryDao.updateName(id, name)
            if (backgroundColor != null) categoryDao.updateBackgroundColor(id, backgroundColor)
        }
    }

    suspend fun totalReset(categories: Flow<List<Category>>) {
        categoryDao.deleteAll()
        categoryDao.insert(categories.first())
    }
}