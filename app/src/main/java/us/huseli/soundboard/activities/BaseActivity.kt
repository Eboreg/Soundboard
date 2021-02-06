package us.huseli.soundboard.activities

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.Application
import java.util.*

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { super.attachBaseContext(updateBaseContextLocale(newBase)) }
    }

    private fun updateBaseContextLocale(context: Context): Context {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("language", "en")
            ?.let { language ->
                val realLanguage =
                    if (language == "default") (context.applicationContext as Application).deviceDefaultLanguage else language
                if (realLanguage != null) {
                    val locale = Locale(realLanguage)
                    Locale.setDefault(locale)
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) return updateResourcesLocale(
                        context,
                        locale
                    )
                    updateResourcesLocaleLegacy(context, locale)
                } else context
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