package us.huseli.soundboard

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.databinding.library.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import us.huseli.soundboard.helpers.ColorHelper
import java.io.File

@HiltAndroidApp
class GlobalApplication : Application() {
    var soundDir: File? = null

    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        application = this
        soundDir = getDir("sounds", Context.MODE_PRIVATE)
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }

    fun getColorHelper(): ColorHelper {
        return colorHelper ?: run { ColorHelper(resources).also { colorHelper = it } }
    }

    companion object {
        const val LOG_TAG = "soundboard"

        // Gonna allow lateinit here; it's totally OK if it throws up an exception
        lateinit var application: GlobalApplication
        private var colorHelper: ColorHelper? = null
    }
}