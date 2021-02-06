package us.huseli.soundboard

import android.app.Application
import android.os.StrictMode
import androidx.databinding.library.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import java.util.*

@HiltAndroidApp
class Application : Application() {
    var deviceDefaultLanguage: String? = null

    override fun onCreate() {
        if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        deviceDefaultLanguage = Locale.getDefault().language
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
        )
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }
}