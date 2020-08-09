package us.huseli.soundboard_kotlin.data

import androidx.lifecycle.LiveData

class SoundRepository(private val soundDao: SoundDao) {
    val sounds: LiveData<List<Sound>> = soundDao.getAll()

    fun soundsByCategory(catId: Int) = soundDao.byCategory(catId)

    fun insert(sound: Sound) {
        // If sounds exist, set sound.order to max order + 1; else 0
        val lastSound = sounds.value?.maxBy { it.order }
        lastSound?.order?.let {
            sound.order = it + 1
        }
        soundDao.insert(sound)
    }

    fun update(sound: Sound) = soundDao.update(sound)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    suspend fun get(soundId: Int) = soundDao.get(soundId)
}
