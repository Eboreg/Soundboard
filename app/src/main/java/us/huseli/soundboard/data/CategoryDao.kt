package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Suppress("FunctionName")
@Dao
interface CategoryDao {
    @Query("SELECT MAX(`order`) FROM SoundCategory")
    fun _getMaxOrder(): Int?

    @Insert
    fun _insert(category: Category)

    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM SoundCategory WHERE id NOT IN (:categoryIds)")
    fun deleteExcluding(categoryIds: List<Int>)

    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun get(id: Int): LiveData<Category?>

    @Query("SELECT backgroundColor FROM SoundCategory")
    fun getUsedColors(): List<Int>

    @Transaction
    fun insert(category: Category) {
        if (category.order == -1) {
            val maxOrder = _getMaxOrder() ?: -1
            category.order = maxOrder
        }
        _insert(category)
    }

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun list(): List<Category>

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun listLive(): LiveData<List<Category>>

    @Transaction
    fun reset(categories: List<Category>) {
        val dbCategories = list()
        categories.forEach {
            if (dbCategories.contains(it)) update(it)
            else insert(it)
        }
        deleteExcluding(categories.mapNotNull { it.id })
    }

    @Transaction
    fun saveOrder(categories: List<Category>) =
            categories.forEach { category -> category.id?.let { categoryId -> updateOrder(categoryId, category.order) } }

    @Query("UPDATE SoundCategory SET collapsed = :value WHERE id = :id")
    fun setCollapsed(id: Int, value: Int)

    @Update
    fun update(category: Category)

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)
}