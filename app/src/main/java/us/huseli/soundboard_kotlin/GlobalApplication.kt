package us.huseli.soundboard_kotlin

import android.app.Application
import android.net.Uri
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

class GlobalApplication : Application() {
    private val players = HashMap<Int, SoundPlayer>()

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    fun getPlayer(soundId: Int, soundUri: Uri, soundVolume: Int): SoundPlayer {
        return players[soundId] ?: SoundPlayer(this, soundUri, soundVolume).apply { players[soundId] = this }
    }

    fun getPlayer(sound: Sound) = getPlayer(sound.id!!, sound.uri, sound.volume)

    fun getPlayer(viewModel: SoundViewModel) = getPlayer(viewModel.id!!, viewModel.uri, viewModel.volume)

    companion object {
        const val LOG_TAG = "sgrumf"

        lateinit var application: GlobalApplication
    }
}