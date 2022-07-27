package us.huseli.soundboard.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.R
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.SoundRepository
import us.huseli.soundboard.data.SoundboardDatabase
import us.huseli.soundboard.databinding.ActivitySettingsBinding
import us.huseli.soundboard.fragments.BackupDialogFragment
import us.huseli.soundboard.fragments.RestoreDialogFragment
import us.huseli.soundboard.helpers.FolderSelectPreference
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

    @Inject
    lateinit var soundRepository: SoundRepository


    /********* OVERRIDDEN METHODS ************************************************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(soundRepository))
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

    override fun onSettingChanged(key: String, value: Any?) {
        when (key) {
            "language" -> setLanguage(value as String)
            "nightMode" -> setNightMode(value as String)
        }
    }


    /********* SUBCLASSES ********************************************************************************************/
    class SettingsFragment(private val soundRepository: SoundRepository) : PreferenceFragmentCompat() {
        private var watchFolderLauncher: ActivityResultLauncher<Intent>? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)
            if (BuildConfig.DEBUG) addPreferencesFromResource(R.xml.settings_debug)

            watchFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                onWatchFolderChanged(result.data?.data)
            }

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

            findPreference<FolderSelectPreference>("watchFolder")?.apply {
                setOnPreferenceClickListener { startSelectWatchFolderActivity() }
            }

            findPreference<Preference>("clearWatchFolder")?.setOnPreferenceClickListener {
                findPreference<FolderSelectPreference>("watchFolder")?.setValue(null)
                true
            }

            findPreference<Preference>("untrashAllSounds")?.setOnPreferenceClickListener {
                soundRepository.untrashAll()
                true
            }

            findPreference<Preference>("deleteAllSounds")?.setOnPreferenceClickListener {
                soundRepository.deleteAll()
                true
            }
        }

        private fun onWatchFolderChanged(uri: Uri?) {
            if (uri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                findPreference<SwitchPreference>("watchFolderTrashMissing")?.isEnabled = true
                findPreference<Preference>("clearWatchFolder")?.isEnabled = true
            }
            else {
                findPreference<SwitchPreference>("watchFolderTrashMissing")?.isEnabled = false
                findPreference<Preference>("clearWatchFolder")?.isEnabled = false
            }
            findPreference<FolderSelectPreference>("watchFolder")?.also { pref ->
                pref.setValue(uri)
            }
        }

        private fun onResetBufferSizeClick(): Boolean {
            findPreference<SeekBarPreference>("bufferSize")?.apply {
                value = Functions.bufferSizeToSeekbarValue(Constants.DEFAULT_BUFFER_SIZE)
                callChangeListener(value)
            }
            return true
        }

        private fun startSelectWatchFolderActivity(): Boolean {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            findPreference<FolderSelectPreference>("watchFolder")?.getValue()?.also {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, it)
            }
            watchFolderLauncher?.launch(intent)
            return true
        }
    }

}
