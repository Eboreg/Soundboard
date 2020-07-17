package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CategoryDao {
    @Insert
    fun insert(category: Category)

    @Update
    fun update(category: Category)

    @Query("SELECT * FROM SoundCategory ORDER BY `order`")
    fun getAll(): LiveData<List<Category>>

}