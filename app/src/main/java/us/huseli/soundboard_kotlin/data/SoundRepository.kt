package us.huseli.soundboard_kotlin.data

import android.graphics.Color
import androidx.lifecycle.map

class SoundRepository(private val soundDao: SoundDao) {
    val sounds= soundDao.getAll()

    fun insert(sound: Sound) = soundDao.insert(sound)

    fun update(sound: Sound) = soundDao.update(sound)

    fun updateOrder(sounds: List<Sound>) = soundDao.updateOrder(sounds)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    fun get(soundId: Int) = soundDao.get(soundId)

    fun getBackgroundColor(categoryId: Int) = soundDao.getBackgroundColor(categoryId).map { it ?: Color.DKGRAY }
}
