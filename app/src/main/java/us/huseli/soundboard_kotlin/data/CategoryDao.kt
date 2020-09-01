package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Insert
    fun insert(category: Category)

    @Update
    fun update(category: Category)

    @Transaction
    fun update(categories: List<CategoryWithSounds>) = categories.forEach { category -> update(category.toCategory()) }

    @Transaction
    fun updateOrder(categories: List<Category>) {
        categories.forEachIndexed { index, category ->
            if (category.order != index) updateOrder(category.id!!, index)
        }
    }

    @Query("UPDATE SoundCategory SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)

    @Transaction
    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    //@Query("SELECT sc.*, COUNT(s.id) as soundCount FROM SoundCategory AS sc LEFT JOIN Sound as s ON sc.id = s.categoryId GROUP BY sc.id ORDER BY sc.`order`, sc.id")
    fun getAllWithSounds(): LiveData<List<CategoryWithSounds>>

    @Query("SELECT * FROM SoundCategory ORDER BY `order`, id")
    //@Query("SELECT sc.*, COUNT(s.id) as soundCount FROM SoundCategory AS sc LEFT JOIN Sound as s ON sc.id = s.categoryId GROUP BY sc.id ORDER BY sc.`order`, sc.id")
    fun getAll(): LiveData<List<Category>>

    @Transaction
    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun getWithSounds(id: Int): LiveData<CategoryWithSounds>

    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun get(id: Int): LiveData<Category>

    @Delete
    fun delete(category: Category)

    fun delete(categoryWithSounds: CategoryWithSounds) = delete(categoryWithSounds.toCategory())

    @Query("DELETE FROM SoundCategory WHERE id = :id")
    fun delete(id: Int)
}