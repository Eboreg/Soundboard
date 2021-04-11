package us.huseli.soundboard.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.helpers.Functions

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.also {
            when (key) {
                "language" -> setLanguage(sharedPreferences.getString(key, "en"))
                "nightMode" -> setNightMode(sharedPreferences.getString(key, "default"))
            }
        }
    }


    class SettingsFragment : PreferenceFragmentCompat(), View.OnClickListener {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.rootView?.findViewWithTag<Button>("resetBufferSize")?.setOnClickListener(this)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            findPreference<SeekBarPreference>("bufferSize")?.apply {
                setDefaultValue(Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))
                summary = Functions.seekbarValueToBufferSize(value).toString()
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    summary = Functions.seekbarValueToBufferSize(newValue as Int).toString()
                    true
                }
            }
        }

        override fun onClick(v: View?) {
            if (v?.tag == "resetBufferSize") {
                findPreference<SeekBarPreference>("bufferSize")?.apply {
                    value = Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)
                    callChangeListener(value)
                }
            }
        }
    }
}
