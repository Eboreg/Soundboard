package us.huseli.soundboard_kotlin

import android.app.Application
import android.net.Uri
import android.os.StrictMode
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.helpers.ColorHelper

class GlobalApplication : Application() {
    //private val players = HashMap<Int, SoundPlayer>()
    private val players = HashMap<Uri, SoundPlayer>()

    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        application = this
        colorHelper = ColorHelper(resources)
    }

    fun getPlayer(uri: Uri): SoundPlayer {
        return players[uri] ?: SoundPlayer(this, uri).apply {
            players[uri] = this
        }
    }

/*
    fun getPlayer(sound: Sound): SoundPlayer {
        return players[sound.id] ?: SoundPlayer(this, sound.uri, sound.volume).apply {
            sound.id?.let { players[it] = this }
        }
    }
 */

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }

    //fun setPlayerVolume(sound: Sound, volume: Int) = players[sound.id]?.setVolume(volume)
    fun setPlayerVolume(sound: Sound, volume: Int) = players[sound.uri]?.setVolume(volume)

    fun deletePlayers(soundUris: List<Uri>?) {
        soundUris?.forEach { uri ->
            players[uri]?.release()
            players.remove(uri)
        }
    }

    companion object {
        const val LOG_TAG = "soundboard"

        // Gonna allow lateinit here; it's totally OK if it throws up an exception
        lateinit var application: GlobalApplication
        lateinit var colorHelper: ColorHelper
    }
}