package us.huseli.soundboard_kotlin

import android.app.Application
import us.huseli.soundboard_kotlin.data.Sound

class GlobalApplication : Application() {
    private val players = HashMap<Int, SoundPlayer>()

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    fun getPlayer(sound: Sound) =
            players[sound.id!!] ?: SoundPlayer(this, sound.uri, sound.volume).apply { players[sound.id!!] = this }

    companion object {
        const val LOG_TAG = "sgrumf"

        lateinit var application: GlobalApplication
    }
}