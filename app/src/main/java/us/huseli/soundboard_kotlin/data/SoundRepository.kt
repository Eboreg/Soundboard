package us.huseli.soundboard_kotlin.data

import android.graphics.Color
import androidx.lifecycle.liveData
import androidx.lifecycle.map

class SoundRepository(private val soundDao: SoundDao) {
    fun insert(sound: Sound) = soundDao.insert(sound)

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    fun get(soundId: Int?) = soundId?.let { soundDao.get(soundId) } ?: liveData { }

    fun getByCategory(categoryId: Int?) = soundDao.getAll().map { list -> list.filter { it.categoryId == categoryId } }

    fun getBackgroundColor(categoryId: Int?)
            = categoryId?.let { soundDao.getBackgroundColor(categoryId).map { it ?: Color.DKGRAY } } ?: liveData { Color.DKGRAY }

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    fun get(soundIds: List<Int>) = soundDao.get(soundIds)

    fun delete(soundIds: List<Int>?) {
        if (soundIds != null) soundDao.delete(soundIds)
    }

}
