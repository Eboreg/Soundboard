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
    fun updateChecksum(soundId: Int?, checksum: String) {
        if (soundId != null) soundDao.updateChecksum(soundId, checksum)
    }

    fun update(sounds: List<Sound>, name: String?, volume: Int, categoryId: Int?) =
        soundDao.update(sounds, name, volume, categoryId)

    fun updateCategoryAndOrder(soundIds: List<Int>, categoryId: Int) =
        /**
         * Updates category, then saves Sound.order according to position in list.
         * List is assumed to contain _all_ sounds now in this category, in their intended order.
         */
        soundDao.updateCategoryAndOrder(soundIds, categoryId)


    /********* DELETE ************************************************************************************************/
    fun delete(soundIds: List<Int>) = soundDao.delete(soundIds)

    fun deleteByCategory(categoryId: Int) = soundDao.deleteByCategory(categoryId)


    /********* VARIOUS ***********************************************************************************************/
    fun sort(categoryId: Int?, sorting: SoundSorting) {
        /** Sorts all sounds within category */
        categoryId?.let { soundDao.sortWithinCategory(it, sorting) }
    }

    fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId)
}
