package us.huseli.soundboard.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(private val soundDao: SoundDao) {
    fun delete(sounds: List<Sound>) = soundDao.delete(sounds.mapNotNull { it.id })

    fun deleteByIds(soundIds: List<Int>) = soundDao.delete(soundIds)

    fun deleteByCategory(categoryId: Int) = delete(soundDao.listByCategory(categoryId))

    fun insert(sound: Sound) = soundDao.insert(sound)

    fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    fun list() = soundDao.list()

    fun listLiveWithCategory() = soundDao.listLiveWithCategory()

    /** Sorts all sounds within category */
    fun sort(categoryId: Int?, sorting: Sound.Sorting) {
        categoryId?.let { soundDao.sort(soundDao.listByCategory(categoryId), sorting) }
    }

    fun update(sound: Sound) = soundDao.update(sound)

    fun update(sounds: List<Sound>) = soundDao.update(sounds)

    fun updateChecksum(sound: Sound, checksum: String?) {
        sound.id?.also { soundDao.updateChecksum(it, checksum) }
    }

    fun update(sounds: List<Sound>, categoryId: Int?) {
        if (categoryId != null) soundDao.update(sounds, categoryId)
        else soundDao.update(sounds)
    }

    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) {
        soundDao.update(sounds, name, volume, categoryId)
    }

    fun update(sound: Sound, name: String?, volume: Int, categoryId: Int?) {
        soundDao.update(sound, name, volume, categoryId)
    }
}
