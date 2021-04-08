package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT MAX(`order`) FROM SoundCategory")
    fun getMaxOrder(): Int?

    @Insert
    fun privateInsert(category: Category)

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
            val maxOrder = getMaxOrder() ?: -1
            category.order = maxOrder + 1
        }
        privateInsert(category)
    }

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun list(): List<Category>

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    fun listLive(): LiveData<List<Category>>

    @Transaction
    fun reset(categories: List<Category>) {
        val dbCategories = list()
        categories.forEach { category ->
            if (dbCategories.contains(category)) {
                // Dont reset collapsed status
                dbCategories.findLast { it == category }?.let { category.collapsed = it.collapsed }
                update(category)
            } else insert(category)
        }
        deleteExcluding(categories.mapNotNull { it.id })
    }

    @Query("UPDATE SoundCategory SET collapsed = :value WHERE id = :id")
    fun setCollapsed(id: Int, value: Int)

    @Transaction
    fun sort(categories: List<Category>) {
        /** Update .order according to current order in list and save */
        categories.forEachIndexed { index, category -> category.order = index }
        update(categories)
    }

    @Update
    fun update(category: Category)

    @Update
    fun update(categories: List<Category>)
}