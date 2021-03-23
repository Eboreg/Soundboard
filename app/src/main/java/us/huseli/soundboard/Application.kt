package us.huseli.soundboard

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.core.content.edit
import dagger.hilt.android.HiltAndroidApp
import us.huseli.soundboard.activities.MainActivity
import us.huseli.soundboard.data.Constants
import java.util.*

@HiltAndroidApp
class Application : Application() {
    var deviceDefaultLanguage: String? = null

    override fun onCreate() {
        // if (BuildConfig.DEBUG) enableStrictMode()
        super.onCreate()
        deviceDefaultLanguage = Locale.getDefault().language
        val prefs = getSharedPreferences(MainActivity::class.qualifiedName, Context.MODE_PRIVATE)
        prefs.edit { remove(Constants.PREF_REPRESS_MODE).apply() }
    }

    @Suppress("unused")
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
        )
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }
}