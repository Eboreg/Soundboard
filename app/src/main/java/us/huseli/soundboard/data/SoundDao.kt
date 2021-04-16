package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SoundDao {
    /********* INSERT ************************************************************************************************/
    @Insert
    fun insert(sounds: List<Sound>)


    /********* LIST **************************************************************************************************/
    @Query("SELECT Sound.* FROM Sound JOIN SoundCategory ON Sound.categoryId = SoundCategory.id ORDER BY SoundCategory.`order`, Sound.`order`")
    fun list(): List<Sound>

    @Query("SELECT * FROM Sound WHERE categoryId = :categoryId ORDER BY `order`")
    fun listByCategory(categoryId: Int): List<Sound>

    @Query("SELECT * FROM Sound")
    fun listLive(): LiveData<List<Sound>>

    @Transaction
    @Query("SELECT Sound.* FROM Sound JOIN SoundCategory ON Sound.categoryId = SoundCategory.id ORDER BY SoundCategory.`order`, Sound.`order`")
    fun listLiveWithCategory(): LiveData<List<SoundWithCategory>>


    /********* UPDATE ************************************************************************************************/
    @Query("UPDATE Sound SET `order`=:order WHERE id=:soundId")
    fun updateOrder(soundId: Int, order: Int)

    @Update
    fun update(sounds: List<Sound>)

    @Query("UPDATE Sound SET duration = :duration WHERE id = :soundId")
    fun updateDuration(soundId: Int, duration: Long)

    @Query("UPDATE Sound SET checksum = :checksum WHERE id = :soundId")
    fun updateChecksum(soundId: Int, checksum: String?)

    @Transaction
    fun updateCategoryAndOrder(sounds: List<Sound>, categoryId: Int) {
        var order = 0
        sounds.forEach { sound -> if (sound.id != null) update(sound.id, categoryId, order++) }
    }

    @Transaction
    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) {
        val batch = sounds.filter { categoryId != null || it.categoryId == categoryId }
        if (categoryId != null) {
            var order = getMaxOrder(categoryId) ?: -1
            sounds.minus(batch).forEach { sound ->
                if (sound.id != null) {
                    if (name != null) update(sound.id, name, volume, categoryId, ++order)
                    else update(sound.id, volume, categoryId, ++order)
                }
            }
        }
        if (name != null) update(batch.mapNotNull { it.id }, name, volume)
        else update(batch.mapNotNull { it.id }, volume)
    }

    @Query("UPDATE Sound SET name=:name, volume=:volume, categoryId=:categoryId, `order`=:order WHERE id=:soundId")
    fun update(soundId: Int, name: String, volume: Int, categoryId: Int, order: Int)

    @Query("UPDATE Sound SET volume=:volume, categoryId=:categoryId, `order`=:order WHERE id=:soundId")
    fun update(soundId: Int, volume: Int, categoryId: Int, order: Int)

    @Query("UPDATE Sound SET categoryId=:categoryId, `order`=:order WHERE id=:soundId")
    fun update(soundId: Int, categoryId: Int, order: Int)

    @Query("UPDATE Sound SET volume=:volume WHERE id IN(:soundIds)")
    fun update(soundIds: List<Int>, volume: Int)

    @Query("UPDATE Sound SET name=:name, volume=:volume WHERE id IN(:soundIds)")
    fun update(soundIds: List<Int>, name: String, volume: Int)


    /********* DELETE ************************************************************************************************/
    @Query("DELETE FROM Sound WHERE id IN (:soundIds)")
    fun delete(soundIds: List<Int>)

    @Query("DELETE FROM Sound WHERE id NOT IN (:soundIds)")
    fun deleteExcluding(soundIds: List<Int>)


    /********* VARIOUS ***********************************************************************************************/
    @Query("SELECT MAX(`order`) FROM Sound WHERE categoryId = :categoryId")
    fun getMaxOrder(categoryId: Int): Int?

    @Transaction
    fun reset(sounds: List<Sound>) {
        val dbSounds = list()
        update(sounds.filter { dbSounds.contains(it) })
        insert(sounds.filterNot { dbSounds.contains(it) })
        deleteExcluding(sounds.mapNotNull { it.id })
    }

    @Transaction
    fun sort(sounds: List<Sound>, sorting: Sound.Sorting) {
        var order = 0
        sounds.sortedWith(Sound.Comparator(sorting.parameter, sorting.order)).forEach { sound ->
            if (sound.id != null) updateOrder(sound.id, order++)
        }
    }

}