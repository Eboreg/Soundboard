package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.LiveData
import java.util.*

class SoundRepository(application: Application) {
    private val soundDao = SoundDatabase.getInstance(application).soundDao()
    val sounds: LiveData<List<Sound>> = soundDao.getAll()

    fun insert(sound: Sound) {
        // If sounds exist, set sound.order to max order + 1; else 0
        val lastSound = sounds.value?.maxBy { it.order }
        lastSound?.order?.let {
            sound.order = it + 1
        }
        soundDao.insert(sound)
    }

    fun updateOrder(fromPosition: Int, toPosition: Int) {
        sounds.value?.let {
            if (fromPosition < toPosition) for (i in fromPosition until toPosition) Collections.swap(it, i, i + 1)
            else for (i in fromPosition downTo toPosition + 1) Collections.swap(it, i, i - 1)
            it.forEachIndexed { idx, s ->
                if (s.order != idx) {
                    s.order = idx
                    soundDao.update(s)
                }
            }
        }
    }

    fun update(sound: Sound) = soundDao.update(sound)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    companion object {
        @Volatile private var instance: SoundRepository? = null

        fun getInstance(application: Application): SoundRepository {
            return instance ?: synchronized(this) {
                instance ?: SoundRepository(application).also { instance = it }
            }
        }
    }
}
