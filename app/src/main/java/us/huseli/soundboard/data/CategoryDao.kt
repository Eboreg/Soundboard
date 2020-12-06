package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Suppress("FunctionName")
@Dao
interface CategoryDao {
    @Insert
    fun _insert(category: Category)

    @Query("SELECT MAX(`order`) FROM SoundCategory")
    fun _getMaxOrder(): Int?

    fun insert(category: Category) {
        val maxOrder = _getMaxOrder() ?: -1
        category.order = maxOrder
        _insert(category)
    }

    @Update
    fun update(category: Category)

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun getAll(): LiveData<List<Category>>

    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun get(id: Int): LiveData<Category?>

    @Query("SELECT backgroundColor FROM SoundCategory")
    fun getUsedColors(): List<Int>

    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)

    @Transaction
    fun saveOrder(categories: List<Category>) =
            categories.forEach { category -> category.id?.let { categoryId -> updateOrder(categoryId, category.order) } }

    @Query("UPDATE SoundCategory SET collapsed = :value WHERE id = :id")
    fun setCollapsed(id: Int, value: Int)
}