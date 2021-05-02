package us.huseli.soundboard.activities

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.Application
import us.huseli.soundboard.R
import us.huseli.soundboard.helpers.SettingsManager
import java.util.*
import kotlin.math.roundToInt

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { super.attachBaseContext(updateBaseContext(newBase)) }
    }

    private fun updateBaseContext(context: Context): Context {
        /**
         * Seems like we can't inject SettingsManager, because it will not yet have been created at this point?
         */
        val settingsManager = SettingsManager.createForContext(context)
        var newContext = context

        val realLanguage =
            if (settingsManager.getLanguage() == "default") (context.applicationContext as Application).deviceDefaultLanguage
            else settingsManager.getLanguage()
        if (realLanguage != null) {
            val locale = Locale.forLanguageTag(realLanguage)
            Locale.setDefault(locale)
            newContext = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) updateResourcesLocale(context, locale)
            else updateResourcesLocaleLegacy(context, locale)
        }

        setNightMode(settingsManager.getNightMode())

        settingsManager.destroy()

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

    fun setNightMode(value: String?) = runOnUiThread {
        when (value) {
            "default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "night" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "day" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun setLanguage(language: String?) = runOnUiThread {
        val realLanguage =
            if (language == "default") (applicationContext as Application).deviceDefaultLanguage else language
        if (realLanguage != null) {
            val locale = Locale.forLanguageTag(realLanguage)
            resources.configuration.setLocale(locale)
            applicationContext.createConfigurationContext(resources.configuration)
            recreate()
        }
    }

    fun showFragment(fragment: Fragment, tag: String? = null) {
        supportFragmentManager
            .beginTransaction()
            .add(fragment, tag)
            .show(fragment)
            .commit()
    }

    fun showProgressOverlay() {
        window.decorView.rootView.post {
            findViewById<ConstraintLayout>(R.id.progressOverlay)?.visibility = View.VISIBLE
        }
    }

    fun hideProgressOverlay() {
        window.decorView.rootView.post {
            findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.GONE
            findViewById<TextView>(R.id.progressText)?.visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.progressOverlay)?.visibility = View.GONE
        }
    }

    fun updateProgress(text: String, currentFileIdx: Int?, totalFileCount: Int?) {
        window.decorView.rootView.post {
            findViewById<TextView>(R.id.progressText)?.let {
                it.visibility = View.VISIBLE
                it.text = text
            }
            findViewById<ProgressBar>(R.id.progressBar)?.let { progressBar ->
                if (totalFileCount == null || totalFileCount == 0) progressBar.visibility = View.GONE
                else if (currentFileIdx != null) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = ((currentFileIdx.toFloat() / totalFileCount) * 100).roundToInt()
                }
            }
        }
    }
}