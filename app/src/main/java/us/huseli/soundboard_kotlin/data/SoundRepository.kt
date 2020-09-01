package us.huseli.soundboard_kotlin.data

class SoundRepository(private val soundDao: SoundDao) {
    val sounds= soundDao.getAll()

    fun insert(sound: Sound) = soundDao.insert(sound)
    fun insert(sound: Sound, order: Int) = soundDao.insert(sound, order)

    fun update(sound: Sound) = soundDao.update(sound)

    fun updateOrder(sounds: List<Sound>) = soundDao.updateOrder(sounds)

    fun delete(soundId: Int) = soundDao.delete(soundId)

    fun get(soundId: Int) = soundDao.get(soundId)

    fun getBackgroundColor(soundId: Int) = soundDao.getBackgroundColor(soundId)
}
