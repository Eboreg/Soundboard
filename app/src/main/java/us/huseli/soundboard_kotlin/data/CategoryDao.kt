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
    @Query("SELECT sc.*, COUNT(s.id) as soundCount FROM SoundCategory AS sc LEFT JOIN Sound as s ON sc.id = s.categoryId GROUP BY sc.id ORDER BY sc.`order`, sc.id")
    fun getAll(): LiveData<List<CategoryExtended>>

    @Query("SELECT * FROM SoundCategory WHERE id = :id")
    fun get(id: Int): LiveData<Category>

    @Delete
    fun delete(category: Category)
}