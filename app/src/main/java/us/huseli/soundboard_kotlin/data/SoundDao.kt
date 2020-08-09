package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SoundDao {
    @Insert
    fun insert(sound: Sound)

    @Update
    fun update(sound: Sound)

    @Query("SELECT s.* FROM Sound s LEFT JOIN SoundCategory sc ON s.categoryId = sc.id ORDER BY sc.`order`, s.`order`")
    fun getAll(): LiveData<List<Sound>>

    @Query("SELECT * FROM Sound WHERE categoryId = :catId ORDER BY `order`")
    fun byCategory(catId: Int): LiveData<List<Sound>>

    @Query("DELETE FROM Sound")
    fun deleteAll()

    @Query("DELETE FROM Sound WHERE id = :soundId")
    fun delete(soundId: Int)

    @Query("SELECT * FROM Sound WHERE id = :soundId")
    suspend fun get(soundId: Int): Sound
    //fun get(soundId: Int): LiveData<Sound>
}