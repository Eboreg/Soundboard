package us.huseli.soundboard.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(private val soundDao: SoundDao) {
    fun delete(sounds: List<Sound>) = soundDao.delete(sounds.mapNotNull { it.id })

    fun delete(soundIds: List<Int>?) = soundIds?.let { soundDao.delete(it) }

    fun deleteByCategory(categoryId: Int) = delete(soundDao.listByCategory(categoryId))

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    fun insert(sound: Sound) = soundDao.insert(sound)

    fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    fun list() = soundDao.list()

    fun listLiveWithCategory() = soundDao.listLiveWithCategory()

    /** Update/add sounds, delete the rest */
    fun reset(sounds: List<Sound>) = soundDao.reset(sounds)

    /** Sorts all sounds within category */
    fun sort(categoryId: Int, sortBy: Sound.SortParameter, sortOrder: Sound.SortOrder) {
        soundDao.sort(soundDao.listByCategory(categoryId), sortBy, sortOrder)
    }

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)
}
