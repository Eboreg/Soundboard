package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SoundDao {
    @Insert
    fun insert(sound: Sound)

    @Update
    fun update(sound: Sound)

    @Query("SELECT * FROM Sound ORDER BY `order`")
    fun getAll(): LiveData<List<Sound>>

    @Query("DELETE FROM Sound")
    fun deleteAll()

    @Query("DELETE FROM Sound WHERE id = :soundId")
    fun delete(soundId: Int)
}