package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SoundCategoryDao {
    @Insert
    fun insert(category: SoundCategory)

    @Update
    fun update(category: SoundCategory)

    @Query("SELECT * FROM SoundCategory ORDER BY `order`")
    fun getAll(): LiveData<List<SoundCategory>>

}