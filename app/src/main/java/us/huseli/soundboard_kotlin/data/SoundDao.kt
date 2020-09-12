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
        val maxOrder = getMaxOrder(sound.categoryId!!) ?: -1
        sound.order = maxOrder + 1
        _insert(sound)
    }

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
    fun get(soundId: Int): LiveData<Sound?>

    @Query("SELECT backgroundColor FROM SoundCategory WHERE id = :categoryId")
    fun getBackgroundColor(categoryId: Int): LiveData<Int?>

    @Query("SELECT categoryId FROM Sound WHERE id = :soundId")
    fun getCategoryId(soundId: Int?): LiveData<Int?>
}