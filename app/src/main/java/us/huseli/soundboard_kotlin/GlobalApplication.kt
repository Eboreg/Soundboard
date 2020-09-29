package us.huseli.soundboard_kotlin

import android.app.Application
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class GlobalApplication : Application() {
    private val players = HashMap<Int, SoundPlayer>()

    override fun onCreate() {
        super.onCreate()
        application = this
        colorHelper = ColorHelper(resources)
    }

    fun getPlayer(sound: Sound): SoundPlayer {
        return players[sound.id] ?: SoundPlayer(this, sound.uri, sound.volume).apply {
            sound.id?.let { players[it] = this }
        }
    }

    fun setPlayerVolume(sound: Sound, volume: Int) = players[sound.id]?.setVolume(volume)

    companion object {
        const val LOG_TAG = "soundboard"

        lateinit var application: GlobalApplication
        lateinit var colorHelper: ColorHelper
    }
}