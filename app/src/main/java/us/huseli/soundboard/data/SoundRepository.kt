package us.huseli.soundboard.data

import us.huseli.soundboard.GlobalApplication

class SoundRepository(private val soundDao: SoundDao) {

    fun delete(sounds: List<Sound>) {
        sounds.forEach { GlobalApplication.application.deleteSound(it) }
        soundDao.delete(sounds.mapNotNull { it.id })
    }

    fun delete(sound: Sound) = delete(listOf(sound))

    fun delete(soundIds: List<Int>?) = soundIds?.let { soundIds ->
        list(soundIds).forEach { GlobalApplication.application.deleteSound(it) }
        soundDao.delete(soundIds)
    }

    fun delete(soundId: Int) = delete(listOf(soundId))

    fun deleteByCategory(categoryId: Int) = delete(listByCategory(categoryId))

    fun get(soundId: Int?) = soundId?.let { soundDao.get(it) }

    fun getLive(soundId: Int) = soundDao.getLive(soundId)

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    fun insert(sound: Sound) = soundDao.insert(sound)

    fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    fun list() = soundDao.list()

    fun list(soundIds: List<Int>) = soundDao.list(soundIds)

    fun listByCategory(categoryId: Int) = soundDao.listByCategory(categoryId)

    fun listLive() = soundDao.listLive()

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)
}
