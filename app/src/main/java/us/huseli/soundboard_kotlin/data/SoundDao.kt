package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SoundDao {
    @Insert
    fun save(sound: Sound)

    @Query("UPDATE Sound SET name = :name WHERE id = :soundId")
    fun updateName(soundId: Int, name: String)

    @Query("SELECT * FROM Sound ORDER BY `order`")
    fun getAll(): LiveData<List<Sound>>

    @Query("DELETE FROM Sound")
    fun deleteAll()

    @Query("DELETE FROM Sound WHERE id = :soundId")
    fun delete(soundId: Int)
}