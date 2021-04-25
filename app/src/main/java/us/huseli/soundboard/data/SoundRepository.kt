package us.huseli.soundboard.data

import androidx.lifecycle.map
import us.huseli.soundboard.helpers.ColorHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(private val soundDao: SoundDao, private val colorHelper: ColorHelper) {
    /********* INSERT ************************************************************************************************/
    fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    fun insert(sound: Sound) = soundDao.insert(sound)


    /********* LIST **************************************************************************************************/
    fun list() = soundDao.list()

    fun listPaths() = soundDao.listPaths()

    fun listLiveExtended() = soundDao.listLiveExtended().map { list ->
        list.onEach { sound ->
            sound.backgroundColor?.also {
                sound.textColor = colorHelper.getColorOnBackground(it)
            }
        }
    }


    /********* UPDATE ************************************************************************************************/
    fun updateChecksum(sound: Sound, checksum: String?) {
        sound.id?.also { soundDao.updateChecksum(it, checksum) }
    }

    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) {
        soundDao.update(sounds, name, volume, categoryId)
    }

    fun updateCategoryAndOrder(soundIds: List<Int>, categoryId: Int) {
        /**
         * Updates category, then saves Sound.order according to position in list.
         * List is assumed to contain _all_ sounds now in this category, in their intended order.
         */
        soundDao.updateCategoryAndOrder(soundIds, categoryId)
    }


    /********* DELETE ************************************************************************************************/
    fun delete(sounds: List<Sound>) = soundDao.delete(sounds.mapNotNull { it.id })

    fun deleteByIds(soundIds: List<Int>) = soundDao.delete(soundIds)

    fun deleteByCategory(categoryId: Int) = soundDao.deleteByCategory(categoryId)


    /********* VARIOUS ***********************************************************************************************/
    fun sort(category: Category?, sorting: SoundSorting) {
        /** Sorts all sounds within category */
        category?.id?.let { soundDao.sortWithinCategory(it, sorting) }
    }
}
