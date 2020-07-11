package us.huseli.soundboard_kotlin.data

import android.app.Application
import androidx.lifecycle.LiveData

class SoundRepository(application: Application) {
    private val soundDao = SoundDatabase.getInstance(application).soundDao()
    val sounds: LiveData<List<Sound>> = soundDao.getAll()

    fun insert(sound: Sound) {
        // If sounds exist, set sound.order to max order + 1
        val lastSound = sounds.value?.maxBy { it.order }
        lastSound?.order?.let {
            sound.order = it + 1
        }
        soundDao.save(sound)
    }

    fun updateName(soundId: Int, name: String) = soundDao.updateName(soundId, name)

    fun delete(soundId: Int) = soundDao.delete(soundId)
}
