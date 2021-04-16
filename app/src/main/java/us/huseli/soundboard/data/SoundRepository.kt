package us.huseli.soundboard.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(private val soundDao: SoundDao) {
    fun delete(sounds: List<Sound>) = soundDao.delete(sounds.mapNotNull { it.id })

    fun deleteByIds(soundIds: List<Int>) = soundDao.delete(soundIds)

    fun deleteByCategory(categoryId: Int) = delete(soundDao.listByCategory(categoryId))

    fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    fun list() = soundDao.list()

    fun listLiveWithCategory() = soundDao.listLiveWithCategory()

    /** Sorts all sounds within category */
    fun sort(category: Category?, sorting: Sound.Sorting) {
        category?.id?.let { soundDao.sort(soundDao.listByCategory(it), sorting) }
    }

    fun updateChecksum(sound: Sound, checksum: String?) {
        sound.id?.also { soundDao.updateChecksum(it, checksum) }
    }

    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) {
        soundDao.update(sounds, name, volume, categoryId)
    }

    fun updateCategoryAndOrder(sounds: List<Sound>, categoryId: Int) {
        /**
         * Updates category, then saves Sound.order according to position in list.
         * List is assumed to contain _all_ sounds now in this category, in their intended order.
         */
        soundDao.updateCategoryAndOrder(sounds, categoryId)
    }

}
