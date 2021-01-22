package us.huseli.soundboard

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.util.*

abstract class LocaleActivity : AppCompatActivity() {
    private fun updateBaseContextLocale(context: Context): Context {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("language", "en")?.let { language ->
            val locale = Locale(language)
            Locale.setDefault(locale)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) return updateResourcesLocale(context, locale)
            updateResourcesLocaleLegacy(context, locale)
        } ?: context
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

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { super.attachBaseContext(updateBaseContextLocale(newBase)) }
    }

    fun setLanguage(language: String?) {
        if (language != null) {
            val locale = Locale.forLanguageTag(language)
            resources.configuration.setLocale(locale)
            applicationContext.createConfigurationContext(resources.configuration)
            recreate()
        }
    }
}