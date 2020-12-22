package us.huseli.soundboard.data

import androidx.lifecycle.liveData

class SoundRepository(private val soundDao: SoundDao) {
    fun insert(sound: Sound) = soundDao.insert(sound)

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    fun get(soundId: Int?) = soundId?.let { soundDao.get(it) }

    fun getLive(soundId: Int?) = soundId?.let { soundDao.getLive(soundId) } ?: liveData { }

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    fun delete(soundIds: List<Int>?) {
        if (soundIds != null) soundDao.delete(soundIds)
    }

    fun list(soundIds: List<Int>?) = soundIds?.let { soundDao.list(it) } ?: emptyList()

    fun listByCategory(categoryId: Int) = soundDao.listByCategory(categoryId)

    fun listLive() = soundDao.listLive()
}
