package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SoundDao {
    @Insert
    fun privateInsert(sound: Sound)

    @Query("DELETE FROM Sound WHERE id IN (:soundIds)")
    fun delete(soundIds: List<Int>)

    @Query("DELETE FROM Sound WHERE id NOT IN (:soundIds)")
    fun deleteExcluding(soundIds: List<Int>)

    @Query("SELECT MAX(`order`) FROM Sound WHERE categoryId = :categoryId")
    fun getMaxOrder(categoryId: Int): Int?

    @Transaction
    fun insert(sound: Sound) {
        if (sound.order == -1) {
            val maxOrder = sound.categoryId?.let { categoryId -> getMaxOrder(categoryId) } ?: -1
            sound.order = maxOrder + 1
        }
        privateInsert(sound)
    }

    @Transaction
    fun insert(sounds: List<Sound>) = sounds.forEach { insert(it) }

    @Query("SELECT Sound.* FROM Sound JOIN SoundCategory ON Sound.categoryId = SoundCategory.id ORDER BY SoundCategory.`order`, Sound.`order`")
    fun list(): List<Sound>

    @Query("SELECT * FROM Sound WHERE categoryId = :categoryId ORDER BY `order`")
    fun listByCategory(categoryId: Int): List<Sound>

    @Query("SELECT * FROM Sound")
    fun listLive(): LiveData<List<Sound>>

    @Transaction
    @Query("SELECT Sound.* FROM Sound JOIN SoundCategory ON Sound.categoryId = SoundCategory.id ORDER BY SoundCategory.`order`, Sound.`order`")
    fun listLiveWithCategory(): LiveData<List<SoundWithCategory>>

    @Transaction
    fun reset(sounds: List<Sound>) {
        val dbSounds = list()
        sounds.forEach {
            if (dbSounds.contains(it)) update(it)
            else insert(it)
        }
        deleteExcluding(sounds.mapNotNull { it.id })
    }

    @Transaction
    fun sort(sounds: List<Sound>, sortBy: Sound.SortParameter, sortOrder: Sound.SortOrder) {
        val sortedSounds = sounds.sortedWith(Sound.Comparator(sortBy, sortOrder)).mapIndexed { index, sound ->
            sound.copy(order = index)
        }
        update(sortedSounds)
    }

    @Update
    fun update(sound: Sound)

    @Update
    fun update(sounds: List<Sound>)

    @Query("UPDATE Sound SET duration = :duration WHERE id = :soundId")
    fun updateDuration(soundId: Int, duration: Int)
}