package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Suppress("FunctionName")
@Dao
interface SoundDao {
    @Insert
    fun _insert(sound: Sound)

    @Query("DELETE FROM Sound WHERE id IN (:soundIds)")
    fun delete(soundIds: List<Int>)

    @Query("DELETE FROM Sound WHERE id NOT IN (:soundIds)")
    fun deleteExcluding(soundIds: List<Int>)

    @Query("SELECT * FROM Sound WHERE id = :soundId")
    fun get(soundId: Int): Sound?

    @Query("SELECT * FROM Sound WHERE id = :soundId")
    fun getLive(soundId: Int): LiveData<Sound>

    @Query("SELECT MAX(`order`) FROM Sound WHERE categoryId = :categoryId")
    fun getMaxOrder(categoryId: Int): Int?

    @Transaction
    fun insert(sound: Sound) {
        if (sound.order == -1) {
            val maxOrder = sound.categoryId?.let { categoryId -> getMaxOrder(categoryId) } ?: -1
            sound.order = maxOrder + 1
        }
        _insert(sound)
    }

    @Transaction
    fun insert(sounds: List<Sound>) = sounds.forEach { insert(it) }

    @Query("SELECT * FROM Sound")
    fun list(): List<Sound>

    @Query("SELECT * FROM Sound WHERE id IN (:soundIds)")
    fun list(soundIds: List<Int>): List<Sound>

    @Query("SELECT * FROM Sound WHERE categoryId = :categoryId")
    fun listByCategory(categoryId: Int): List<Sound>

    @Query("SELECT * FROM Sound ORDER BY `order`")
    fun listLive(): LiveData<List<Sound>>

    @Transaction
    fun reset(sounds: List<Sound>) {
        val dbSounds = list()
        sounds.forEach {
            if (dbSounds.contains(it)) update(it)
            else insert(it)
        }
        deleteExcluding(sounds.mapNotNull { it.id })
    }

    @Update
    fun update(sound: Sound)

    @Transaction
    fun update(sounds: List<Sound>) = sounds.forEach { sound -> update(sound) }

    @Query("UPDATE Sound SET duration = :duration WHERE id = :soundId")
    fun updateDuration(soundId: Int, duration: Int)
}