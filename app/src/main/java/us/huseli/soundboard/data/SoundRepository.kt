@file:Suppress("RedundantSuspendModifier")

package us.huseli.soundboard.data

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.soundboard.helpers.ColorHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundRepository @Inject constructor(private val soundDao: SoundDao, private val colorHelper: ColorHelper) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    /********* INSERT ************************************************************************************************/
    suspend fun insert(sounds: List<Sound>) = soundDao.insert(sounds)

    suspend fun insert(sound: Sound) = soundDao.insert(sound)


    /********* LIST **************************************************************************************************/

    /** Only used in SoundViewModel.moveFilesToLocalStorage() */
    suspend fun list(): List<Sound> = soundDao.list()

    suspend fun listAll(): List<Sound> = soundDao.listAll()

    fun listExtended() = soundDao.listExtended().map { list ->
        list.onEach { sound ->
            sound.backgroundColor?.also { sound.textColor = colorHelper.getColorOnBackground(it) }
        }
    }

    fun listExtendedByCategory(categoryId: Int) = listExtended().map { list ->
        list.filter { it.categoryId == categoryId }
    }

    fun listLiveExtended() = listExtended().asLiveData()


    /********* UPDATE ************************************************************************************************/
    suspend fun updateChecksum(soundId: Int?, checksum: String) {
        if (soundId != null) soundDao.updateChecksum(soundId, checksum)
    }

    suspend fun update(sounds: List<Sound>, name: String?, volume: Int?, categoryId: Int?) =
        soundDao.update(sounds, name, volume ?: Constants.DEFAULT_VOLUME, categoryId)

    /**
     * Updates category, then saves Sound.order according to position in list. List is assumed to contain _all_ sounds
     * now in this category, in their intended order.
     */
    suspend fun updateCategoryAndOrder(soundIds: List<Int>, categoryId: Int) =
        soundDao.updateCategoryAndOrder(soundIds, categoryId)


    /********* DELETE ************************************************************************************************/
    suspend fun delete(soundIds: List<Int>) = soundDao.delete(soundIds)

    suspend fun deleteByCategory(categoryId: Int) = soundDao.deleteByCategory(categoryId)

    suspend fun trash(soundIds: List<Int>) = soundDao.trash(soundIds)

    suspend fun untrash(soundIds: List<Int>) = soundDao.untrash(soundIds)

    fun deleteAll() = scope.launch { soundDao.deleteAll() }

    fun untrashAll() = scope.launch { soundDao.untrashAll() }


    /********* VARIOUS ***********************************************************************************************/
    suspend fun sort(categoryId: Int?, sorting: SoundSorting) {
        /** Sorts all sounds within category */
        categoryId?.let { soundDao.sortWithinCategory(it, sorting) }
    }

    suspend fun getMaxOrder(categoryId: Int) = soundDao.getMaxOrder(categoryId)

    suspend fun totalReset(sounds: List<Sound>) {
        soundDao.deleteAll()
        soundDao.insert(sounds)
    }
}
