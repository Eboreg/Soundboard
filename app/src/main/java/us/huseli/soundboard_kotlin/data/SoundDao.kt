package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SoundDao {
    @Insert
    fun insert(sound: Sound)

    fun insert(sound: Sound, order: Int) = insert(Sound(sound, order))

    @Update
    fun update(sound: Sound)

    @Transaction
    fun updateOrder(sounds: List<Sound>) {
        sounds.forEachIndexed { index, sound ->
            if (sound.order != index) updateOrder(sound.id!!, index)
        }
    }

    @Query("UPDATE Sound SET `order` = :order WHERE id = :soundId")
    fun updateOrder(soundId: Int, order: Int)

    @Query("SELECT * FROM Sound ORDER BY `order`")
    fun getAll(): LiveData<List<Sound>>

    @Query("DELETE FROM Sound WHERE id = :soundId")
    fun delete(soundId: Int)

    @Query("SELECT * FROM Sound WHERE id = :soundId")
    fun get(soundId: Int): LiveData<Sound>

    @Query("SELECT c.backgroundColor FROM SoundCategory c, Sound s WHERE c.id = s.categoryId AND s.id = :soundId")
    fun getBackgroundColor(soundId: Int): LiveData<Int>
}