package us.huseli.soundboard_kotlin.data

import android.graphics.Color
import androidx.lifecycle.liveData
import androidx.lifecycle.map

class SoundRepository(private val soundDao: SoundDao) {
    //val sounds= soundDao.getAll()

    fun insert(sound: Sound) = soundDao.insert(sound)

    fun update(sound: Sound) = soundDao.update(sound)

    fun updateOrder(sounds: List<Sound>) = soundDao.updateOrder(sounds)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    fun get(soundId: Int) = soundDao.get(soundId)

    fun getByCategory(categoryId: Int?) = soundDao.getAll().map { list -> list.filter { it.categoryId == categoryId } }

    fun getBackgroundColor(categoryId: Int?)
            = categoryId?.let { soundDao.getBackgroundColor(categoryId).map { it ?: Color.DKGRAY } } ?: liveData { Color.DKGRAY }

    fun getCategoryId(sound: Sound) = soundDao.getCategoryId(sound.id)

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId) ?: 0

    //fun getBackgroundColor(sound: Sound)
}
