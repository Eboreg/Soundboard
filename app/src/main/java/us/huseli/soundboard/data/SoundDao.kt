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
    fun sort(sounds: List<Sound>, sorting: Sound.Sorting) {
        val sortedSounds =
            sounds.sortedWith(Sound.Comparator(sorting.parameter, sorting.order)).mapIndexed { index, sound ->
                sound.copy(order = index)
            }
        update(sortedSounds)
    }

    @Update
    fun update(sound: Sound)

    @Update
    fun update(sounds: List<Sound>)

    @Query("UPDATE Sound SET `order` = :order, categoryId = :categoryId WHERE id = :soundId")
    fun update(soundId: Int, order: Int, categoryId: Int)

    @Query("UPDATE Sound SET duration = :duration WHERE id = :soundId")
    fun updateDuration(soundId: Int, duration: Long)

    @Transaction
    fun update(sounds: List<Sound>, categoryId: Int) {
        var order = getMaxOrder(categoryId) ?: -1

        sounds.forEach { sound ->
            if (sound.categoryId != categoryId) update(sound.id!!, ++order, categoryId)
        }

        update(sounds)
    }

    @Query("UPDATE Sound SET checksum = :checksum WHERE id = :soundId")
    fun updateChecksum(soundId: Int, checksum: String?)

    @Transaction
    fun update(sound: Sound, name: String?, volume: Int, categoryId: Int?) {
        if (sound.id != null) {
            if (name != null) updateName(sound.id, name)
            if (categoryId != null) updateCategory(sound.id, categoryId)
            updateVolume(sound.id, volume)
        }
    }

    @Transaction
    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) {
        var order = if (categoryId != null) getMaxOrder(categoryId) ?: -1 else -1

        sounds.forEach { sound ->
            if (sound.id != null) {
                if (name != null) updateName(sound.id, name)
                if (categoryId != null) {
                    if (sound.categoryId != categoryId) updateCategory(sound.id, categoryId, ++order)
                    else updateCategory(sound.id, categoryId)
                }
                updateVolume(sound.id, volume)
            }
        }
    }

    @Query("UPDATE Sound SET volume = :volume WHERE id = :soundId")
    fun updateVolume(soundId: Int, volume: Int)

    @Query("UPDATE Sound SET categoryId = :categoryId WHERE id = :soundId")
    fun updateCategory(soundId: Int, categoryId: Int)

    @Query("UPDATE Sound SET categoryId = :categoryId, `order` = :order WHERE id = :soundId")
    fun updateCategory(soundId: Int, categoryId: Int, order: Int)

    @Query("UPDATE Sound SET name = :name WHERE id = :soundId")
    fun updateName(soundId: Int, name: String)
}