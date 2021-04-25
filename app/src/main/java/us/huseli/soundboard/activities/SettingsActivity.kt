package us.huseli.soundboard.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Settings
import us.huseli.soundboard.data.SoundboardDatabase
import us.huseli.soundboard.databinding.ActivitySettingsBinding
import us.huseli.soundboard.fragments.BackupDialogFragment
import us.huseli.soundboard.fragments.RestoreDialogFragment
import us.huseli.soundboard.helpers.Functions
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: Settings

    @Inject
    lateinit var existingDb: SoundboardDatabase


    /********* OVERRIDDEN METHODS ************************************************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = Settings.fromPrefs(PreferenceManager.getDefaultSharedPreferences(this))

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                "language" -> {
                    val language = sharedPreferences.getString(key, Constants.DEFAULT_LANGUAGE)
                    settings.setLanguage(language)
                    setLanguage(language)
                }
                "nightMode" -> {
                    val nightMode = sharedPreferences.getString(key, "default")
                    settings.setNightMode(nightMode)
                    setNightMode(nightMode)
                }
                "bufferSize" -> settings.setBufferSize(sharedPreferences.getInt("bufferSize", -1))
            }
        }
    }


    /********* SUBCLASSES ********************************************************************************************/

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            findPreference<Preference>("backup")?.setOnPreferenceClickListener {
                (requireActivity() as BaseActivity).showFragment(BackupDialogFragment())
                true
            }
            findPreference<Preference>("restore")?.setOnPreferenceClickListener {
                (requireActivity() as BaseActivity).showFragment(RestoreDialogFragment())
                true
            }
            findPreference<Preference>("resetBufferSize")?.setOnPreferenceClickListener { onResetBufferSizeClick() }

            findPreference<SeekBarPreference>("bufferSize")?.apply {
                setDefaultValue(Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))
                summary = Functions.seekbarValueToBufferSize(value).toString()
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    summary = Functions.seekbarValueToBufferSize(newValue as Int).toString()
                    true
                }
            }
        }

        private fun onResetBufferSizeClick(): Boolean {
            findPreference<SeekBarPreference>("bufferSize")?.apply {
                value = Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)
                callChangeListener(value)
            }
            return true
        }
    }
}
