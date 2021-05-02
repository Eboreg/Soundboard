package us.huseli.soundboard.activities

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.SoundboardDatabase
import us.huseli.soundboard.databinding.ActivitySettingsBinding
import us.huseli.soundboard.fragments.BackupDialogFragment
import us.huseli.soundboard.fragments.RestoreDialogFragment
import us.huseli.soundboard.helpers.Functions
import us.huseli.soundboard.helpers.SettingsManager
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity(), SettingsManager.Listener {
    private lateinit var binding: ActivitySettingsBinding

    @Inject
    lateinit var existingDb: SoundboardDatabase

    @Inject
    lateinit var settingsManager: SettingsManager


    /********* OVERRIDDEN METHODS ************************************************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        settingsManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        settingsManager.registerListener(this)
    }

    override fun onSettingChanged(key: String, value: Any) {
        when (key) {
            Constants.PREF_LANGUAGE -> setLanguage(value as String)
            Constants.PREF_NIGHT_MODE -> setNightMode(value as String)
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

            findPreference<SeekBarPreference>(Constants.PREF_BUFFER_SIZE)?.apply {
                setDefaultValue(Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE))
                summary = Functions.seekbarValueToBufferSize(value).toString()
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    summary = Functions.seekbarValueToBufferSize(newValue as Int).toString()
                    true
                }
            }
        }

        private fun onResetBufferSizeClick(): Boolean {
            findPreference<SeekBarPreference>(Constants.PREF_BUFFER_SIZE)?.apply {
                value = Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)
                callChangeListener(value)
            }
            return true
        }
    }

}
