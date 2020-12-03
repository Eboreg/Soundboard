package us.huseli.soundboard.data

import androidx.lifecycle.liveData
import androidx.lifecycle.map

class SoundRepository(private val soundDao: SoundDao) {
    fun insert(sound: Sound) = soundDao.insert(sound)

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    fun get(soundId: Int?) = soundId?.let { soundDao.get(it) }

    fun getLiveData(soundId: Int?) = soundId?.let { soundDao.getLiveData(soundId) } ?: liveData { }

    fun getByCategory(categoryId: Int?) = soundDao.getAll().map { list -> list.filter { it.categoryId == categoryId } }

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    fun getList(soundIds: List<Int>?) = soundIds?.let { soundDao.get(it) } ?: emptyList()

    fun delete(soundIds: List<Int>?) {
        if (soundIds != null) soundDao.delete(soundIds)
    }

}
