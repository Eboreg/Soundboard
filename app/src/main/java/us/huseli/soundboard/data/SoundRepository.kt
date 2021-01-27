package us.huseli.soundboard.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(private val soundDao: SoundDao) {
    fun delete(sounds: List<Sound>) = soundDao.delete(sounds.mapNotNull { it.id })

    fun delete(sound: Sound) = delete(listOf(sound))

    fun delete(soundIds: List<Int>?) = soundIds?.let { soundDao.delete(it) }

    fun deleteByCategory(categoryId: Int) = delete(listByCategory(categoryId))

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    fun insert(sound: Sound) = soundDao.insert(sound)

    fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    fun list() = soundDao.list()

    fun listByCategory(categoryId: Int) = soundDao.listByCategory(categoryId)

    fun listLive() = soundDao.listLive()

    /** Update/add sounds, delete the rest */
    fun reset(sounds: List<Sound>) = soundDao.reset(sounds)

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)

    fun updateDuration(sound: Sound, duration: Int) =
            sound.id?.let { soundDao.updateDuration(it, duration) }
}
