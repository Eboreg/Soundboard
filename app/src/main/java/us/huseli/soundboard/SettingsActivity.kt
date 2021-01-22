package us.huseli.soundboard

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import kotlin.math.pow
import kotlin.math.roundToInt

class SettingsActivity : LocaleActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment())
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.also {
            when (key) {
                "language" -> setLanguage(sharedPreferences.getString(key, "en"))
                "nightMode" -> {
                    when (sharedPreferences.getString(key, "default")) {
                        "default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        "night" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        "day" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private fun seekbarValueToBufferSize(value: Int) = (11025 * 2.0.pow(value.toDouble())).roundToInt()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)
            findPreference<SeekBarPreference>("bufferSize")?.apply {
                summary = getString(R.string.buffer_size_summary, seekbarValueToBufferSize(value))
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    summary = getString(R.string.buffer_size_summary, seekbarValueToBufferSize(newValue as Int))
                    true
                }
            }
        }

    }
}