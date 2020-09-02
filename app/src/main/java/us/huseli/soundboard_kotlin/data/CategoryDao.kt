package us.huseli.soundboard_kotlin.data

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
        _insert(Category(category, maxOrder + 1))
    }

    @Update
    fun update(category: Category)

    @Transaction
    fun updateOrder(categories: List<Category>) {
        categories.forEachIndexed { index, category ->
            if (category.order != index) updateOrder(category.id!!, index)
        }
    }

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun getAll(): LiveData<List<Category>>

    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun get(id: Int): LiveData<Category>

    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)
}