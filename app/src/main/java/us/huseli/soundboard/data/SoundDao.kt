package us.huseli.soundboard.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SoundDao {
    /********* INSERT ************************************************************************************************/
    @Insert
    fun insert(sounds: List<Sound>)

    @Insert
    fun insert(sound: Sound)


    /********* LIST **************************************************************************************************/
    @Query("SELECT Sound.* FROM Sound JOIN SoundCategory ON Sound.categoryId = SoundCategory.id ORDER BY SoundCategory.`order`, Sound.`order`")
    fun list(): List<Sound>

    @Query("SELECT * FROM Sound WHERE categoryId = :categoryId ORDER BY `order`")
    fun listByCategory(categoryId: Int): List<Sound>

    @Query("SELECT * FROM Sound")
    fun listLive(): LiveData<List<Sound>>

    @Query("SELECT Sound.*, SoundCategory.backgroundColor FROM Sound JOIN SoundCategory ON Sound.categoryId = SoundCategory.id ORDER BY SoundCategory.`order`, Sound.`order`")
    fun listLiveExtended(): LiveData<List<SoundExtended>>

    @Query("SELECT path FROM Sound")
    fun listPaths(): List<String>


    /********* UPDATE ************************************************************************************************/
    @Update
    fun update(sounds: List<Sound>)

    @Query("UPDATE Sound SET volume=:volume, categoryId=:categoryId, `order`=:order WHERE id=:id")
    fun update(id: Int, volume: Int, categoryId: Int, order: Int)

    @Query("UPDATE Sound SET name=:name, volume=:volume, categoryId=:categoryId, `order`=:order WHERE id=:id")
    fun update(id: Int, name: String, volume: Int, categoryId: Int, order: Int)

    @Query("UPDATE Sound SET categoryId=:categoryId, `order`=:order WHERE id=:id")
    fun updateCategoryAndOrder(id: Int, categoryId: Int, order: Int)

    @Transaction
    fun updateCategoryAndOrder(soundIds: List<Int>, categoryId: Int) =
        soundIds.forEachIndexed { index, soundId -> updateCategoryAndOrder(soundId, categoryId, index) }

    @Query("UPDATE Sound SET checksum = :checksum WHERE id = :id")
    fun updateChecksum(id: Int, checksum: String)

    @Query("UPDATE Sound SET duration = :duration WHERE id = :id")
    fun updateDuration(id: Int, duration: Long)

    @Query("UPDATE Sound SET name=:name, volume=:volume WHERE id IN(:ids)")
    fun updateNameAndVolume(ids: List<Int>, name: String, volume: Int)

    @Query("UPDATE Sound SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)

    @Query("UPDATE Sound SET volume=:volume WHERE id IN(:ids)")
    fun updateVolume(ids: List<Int>, volume: Int)

    @Transaction
    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) {
        /**
         * First change category for those sounds that didn't have this category before, placing them last.
         * Then set any other desired properties for those as well as the others.
         */
        val batch = sounds.filter { categoryId == null || it.categoryId == categoryId }
        if (categoryId != null) {
            var order = getMaxOrder(categoryId)
            sounds.minus(batch).mapNotNull { it.id }.forEach { soundId ->
                if (name != null) update(soundId, name, volume, categoryId, ++order)
                else update(soundId, volume, categoryId, ++order)
            }
        }
        if (name != null) updateNameAndVolume(batch.mapNotNull { it.id }, name, volume)
        else updateVolume(batch.mapNotNull { it.id }, volume)
    }


    /********* DELETE ************************************************************************************************/
    @Query("DELETE FROM Sound WHERE id IN (:ids)")
    fun delete(ids: List<Int>)

    @Query("DELETE FROM Sound")
    fun deleteAll()

    @Query("DELETE FROM Sound WHERE id NOT IN (:ids)")
    fun deleteExcluding(ids: List<Int>)

    @Query("DELETE FROM Sound WHERE categoryId = :categoryId")
    fun deleteByCategory(categoryId: Int)


    /********* VARIOUS ***********************************************************************************************/
    @Query("SELECT IFNULL(MAX(`order`), -1) FROM Sound WHERE categoryId = :categoryId")
    fun getMaxOrder(categoryId: Int): Int

    @Transaction
    fun applyState(sounds: List<Sound>) {
        val dbSounds = list()
        update(sounds.filter { dbSounds.contains(it) })
        insert(sounds.filterNot { dbSounds.contains(it) })
        deleteExcluding(sounds.mapNotNull { it.id })
    }

    @Transaction
    fun sortWithinCategory(categoryId: Int, sorting: SoundSorting) {
        val sounds = listByCategory(categoryId)
        sounds.sortedWith(Sound.Comparator(sorting))
            .filterNot { it.id == null }
            .forEachIndexed { index, sound -> updateOrder(sound.id!!, index) }
    }

    @Transaction
    fun totalReset(sounds: List<Sound>) {
        deleteAll()
        insert(sounds)
    }
}