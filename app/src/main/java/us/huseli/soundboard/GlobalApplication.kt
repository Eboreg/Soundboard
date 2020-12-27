package us.huseli.soundboard

import android.app.Application
import android.os.StrictMode
import android.util.Log
import androidx.databinding.library.BuildConfig
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.ColorHelper

class GlobalApplication : Application() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        application = this
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }

    fun getColorHelper(): ColorHelper {
        return colorHelper ?: run { ColorHelper(resources).also { colorHelper = it } }
    }

    fun deleteSound(sound: Sound) {
        try {
            applicationContext.deleteFile(sound.uri.lastPathSegment)
        } catch (e: Exception) {
            Log.w(LOG_TAG, e.toString())
        }
    }

    companion object {
        const val LOG_TAG = "soundboard"

        // Gonna allow lateinit here; it's totally OK if it throws up an exception
        lateinit var application: GlobalApplication
        private var colorHelper: ColorHelper? = null
    }
}