package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Suppress("FunctionName")
@Dao
interface SoundDao {
    @Insert
    fun _insert(sound: Sound)

    @Query("SELECT MAX(`order`) FROM Sound WHERE categoryId = :categoryId")
    fun getMaxOrder(categoryId: Int): Int?

    fun insert(sound: Sound) {
        val maxOrder = sound.categoryId?.let { categoryId -> getMaxOrder(categoryId) } ?: -1
        sound.order = maxOrder + 1
        _insert(sound)
    }

    @Update
    fun update(sound: Sound)

    @Transaction
    fun update(sounds: List<Sound>) = sounds.forEach { sound -> update(sound) }

    @Query("SELECT * FROM Sound ORDER BY `order`")
    fun getAll(): LiveData<List<Sound>>

    @Query("DELETE FROM Sound WHERE id = :soundId")
    fun delete(soundId: Int)

    @Query("SELECT * FROM Sound WHERE id = :soundId")
    fun get(soundId: Int): LiveData<Sound?>

    @Query("SELECT backgroundColor FROM SoundCategory WHERE id = :categoryId")
    fun getBackgroundColor(categoryId: Int): LiveData<Int?>
}