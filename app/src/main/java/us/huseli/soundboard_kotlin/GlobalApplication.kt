package us.huseli.soundboard_kotlin

import android.app.Application
import android.net.Uri
import us.huseli.soundboard_kotlin.data.Sound

class GlobalApplication : Application() {
    private val players = HashMap<Int, SoundPlayer>()

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    private fun getPlayer(soundId: Int?, soundUri: Uri, soundVolume: Int): SoundPlayer {
        return players[soundId] ?: SoundPlayer(this, soundUri, soundVolume).apply {
            if (soundId != null)
                players[soundId] = this
        }
    }

    fun getPlayer(sound: Sound) = getPlayer(sound.id, sound.uri, sound.volume)

    fun setPlayerVolume(sound: Sound, volume: Int) = players[sound.id]?.setVolume(volume)

    companion object {
        const val LOG_TAG = "sgrumf"

        lateinit var application: GlobalApplication
    }
}