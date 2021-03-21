package us.huseli.soundboard.activities

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.Application
import java.util.*

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { super.attachBaseContext(updateBaseContext(newBase)) }
    }

    private fun updateBaseContext(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var newContext = context

        prefs.getString("language", "en")?.let { language ->
            val realLanguage =
                if (language == "default") (context.applicationContext as Application).deviceDefaultLanguage else language
            if (realLanguage != null) {
                val locale = Locale.forLanguageTag(realLanguage)
                Locale.setDefault(locale)
                newContext = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) updateResourcesLocale(context, locale)
                else updateResourcesLocaleLegacy(context, locale)
            }
        }

        setNightMode(prefs.getString("nightMode", "default"))

        return newContext
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        context.resources.configuration.locale = locale
        context.resources.updateConfiguration(resources.configuration, resources.displayMetrics)
        return context
    }

    fun setNightMode(value: String?) {
        when (value) {
            "default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "night" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "day" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun setLanguage(language: String?) {
        val realLanguage =
            if (language == "default") (applicationContext as Application).deviceDefaultLanguage else language
        if (realLanguage != null) {
            val locale = Locale.forLanguageTag(realLanguage)
            resources.configuration.setLocale(locale)
            applicationContext.createConfigurationContext(resources.configuration)
            recreate()
        }
    }
}