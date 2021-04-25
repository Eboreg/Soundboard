package us.huseli.soundboard.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import us.huseli.soundboard.R
import us.huseli.soundboard.activities.BaseActivity
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Settings
import us.huseli.soundboard.data.SoundboardDatabase
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject

@AndroidEntryPoint
class RestoreDialogFragment : DialogFragment() {
    private var getRestoreFileLauncher: ActivityResultLauncher<Intent>? = null
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private lateinit var dialog: AlertDialog

    @Inject
    lateinit var existingDb: SoundboardDatabase

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        getRestoreFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(requireActivity() as BaseActivity)
            doRestore(it.data, it.resultCode)
        }

        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.restore_settings_and_data)
            .setMessage(getString(R.string.restore_description))
            .setPositiveButton(R.string.do_it, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onRestoreConfirmClick() }
        }
        return dialog
    }

    private fun onRestoreConfirmClick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        getRestoreFileLauncher?.launch(intent)
        requireActivity().overridePendingTransition(0, 0)
    }

    private fun restoreDatabase(uri: Uri, currentFileIdx: Int?, totalFileCount: Int?): Int? {
        /** Make sure to run this on Dispatchers.IO */
        updateProgress(getString(R.string.restoring_database), currentFileIdx, totalFileCount)

        ZipInputStream(requireActivity().contentResolver.openInputStream(uri)).use { zipIn ->
            val data = ByteArray(Constants.ZIP_BUFFER_SIZE)
            do {
                val entry = zipIn.nextEntry
                if (entry?.name == "${Constants.ZIP_DB_DIR}/${Constants.DATABASE_NAME}") {
                    val dbFile =
                        File(requireActivity().getDir(Constants.BACKUP_TEMP_DIRNAME, Context.MODE_PRIVATE),
                            Constants.DATABASE_NAME)
                    FileOutputStream(dbFile).use { outputStream ->
                        while (zipIn.available() == 1) {
                            val length = zipIn.read(data)
                            if (length > 0) outputStream.write(data, 0, length)
                        }
                    }
                    val newDb = SoundboardDatabase.buildFromFile(
                        requireContext(), dbFile, Constants.DATABASE_TEMP_NAME)
                    try {
                        existingDb.categoryDao().totalReset(newDb.categoryDao().list())
                        existingDb.soundDao().totalReset(newDb.soundDao().list())
                    } finally {
                        newDb.close()
                        dbFile.delete()
                        requireActivity().getDatabasePath(Constants.DATABASE_TEMP_NAME).delete()
                    }
                    return currentFileIdx?.let { it + 1 }
                }
            } while (entry != null)
        }
        throw FileNotFoundException(getString(R.string.no_database_file_found))
    }

    private fun restoreSounds(uri: Uri, startFileIdx: Int?, totalFileCount: Int?) {
        /** Make sure to run this on Dispatchers.IO */
        var currentFileIdx = startFileIdx

        ZipInputStream(requireActivity().contentResolver.openInputStream(uri)).use { zipIn ->
            val data = ByteArray(Constants.ZIP_BUFFER_SIZE)
            do {
                val entry = zipIn.nextEntry
                if (entry?.name?.startsWith("${Constants.ZIP_SOUNDS_DIR}/") == true && !entry.isDirectory) {
                    val soundFileName = entry.name.substringAfter('/')
                    if (soundFileName.isNotEmpty()) {
                        updateProgress(getString(R.string.restoring_sounds), currentFileIdx, totalFileCount)
                        currentFileIdx = currentFileIdx?.plus(1)
                        val soundFile =
                            File(requireActivity().getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE), soundFileName)
                        FileOutputStream(soundFile).use { outputStream ->
                            while (zipIn.available() == 1) {
                                val length = zipIn.read(data)
                                if (length > 0) outputStream.write(data, 0, length)
                            }
                        }
                    }
                }
            } while (entry != null)
        }
    }

    private fun restoreSettings(uri: Uri, currentFileIdx: Int?, totalFileCount: Int?): Int? {
        updateProgress(getString(R.string.restoring_settings), currentFileIdx, totalFileCount)

        ZipInputStream(requireActivity().contentResolver.openInputStream(uri)).use { zipIn ->
            val data = ByteArray(Constants.ZIP_BUFFER_SIZE)
            do {
                val entry = zipIn.nextEntry
                if (entry?.name == "/${Constants.ZIP_PREFS_FILENAME}") {
                    var json = ""
                    while (zipIn.available() == 1) {
                        val length = zipIn.read(data)
                        if (length > 0) json += String(data, 0, length)
                    }
                    val settings = Settings.fromJson(json)
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
                        putInt("bufferSize", settings.bufferSize)
                        putString("language", settings.language)
                        putString("nightMode", settings.nightMode)
                        apply()
                    }
                    return currentFileIdx?.let { it + 1 }
                }
            } while (entry != null)
        }
        throw FileNotFoundException(getString(R.string.no_settings_file_found))
    }

    private fun doRestore(intent: Intent?, resultCode: Int) {
        /**
         * Is started from getRestoreFileLauncher when returning from ACTION_GET_CONTENT
         */
        val uri = intent?.data
        val fragment = StatusDialogFragment().setTitle(getString(R.string.restoration_results))
        fragment.setOnPositiveButton { dismiss() }

        if (resultCode == Activity.RESULT_OK && uri != null) {
            /**
             * If database restore fails, just quit the whole thing at once and display an error message.
             * On failure to restore other stuff, collect the messages and display a warning.
             */
            dialog.hide()
            showProgressOverlay()

            val zipFile = zipUriToFile(uri)
            val totalFileCount = try {
                ZipFile(zipFile).size()
            } catch (e: Exception) {
                null
            }

            var currentFileIdx: Int? = 0
            scope.launch {
                try {
                    currentFileIdx = restoreSettings(uri, currentFileIdx, totalFileCount)
                    fragment.addMessage(StatusDialogFragment.Status.SUCCESS, getString(R.string.settings_restored))
                } catch (e: Exception) {
                    fragment.addException(e)
                    fragment.addMessage(
                        StatusDialogFragment.Status.WARNING, getString(R.string.could_not_restore_settings))
                }

                val dbRestored = try {
                    currentFileIdx = restoreDatabase(uri, currentFileIdx, totalFileCount)
                    fragment.addMessage(StatusDialogFragment.Status.SUCCESS, getString(R.string.database_restored))
                    fragment.setOnPositiveButton {
                        requireActivity().recreate()
                        dismiss()
                    }
                    true
                } catch (e: Exception) {
                    fragment.addException(e)
                    fragment.addMessage(StatusDialogFragment.Status.ERROR, getString(R.string.could_not_restore_db))
                    false
                }

                if (dbRestored) {
                    try {
                        restoreSounds(uri, currentFileIdx, totalFileCount)
                        fragment.addMessage(StatusDialogFragment.Status.SUCCESS, getString(R.string.sounds_restored))
                    } catch (e: Exception) {
                        fragment.addException(e)
                        fragment.addMessage(
                            StatusDialogFragment.Status.WARNING, getString(R.string.could_not_restore_sounds))
                    }
                }

                hideProgressOverlay()
                (requireActivity() as BaseActivity).showFragment(fragment)
            }
        } else {
            fragment.addMessage(StatusDialogFragment.Status.ERROR, getString(R.string.no_file_chosen))
            dismiss()
            (requireActivity() as BaseActivity).showFragment(fragment)
        }
    }

    private fun zipUriToFile(uri: Uri): File? {
        return try {
            uri.path?.let { path ->
                val outputFile =
                    File(requireActivity().getDir(Constants.BACKUP_TEMP_DIRNAME, Context.MODE_PRIVATE),
                        path.substringAfterLast('/'))
                val buffer = ByteArray(Constants.ZIP_BUFFER_SIZE)
                requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        var len: Int
                        while (inputStream.read(buffer).also { len = it } > 0) {
                            outputStream.write(buffer, 0, len)
                        }
                    }
                }
                return outputFile
            }
            null
        } catch (e: Exception) {
            return null
        }
    }

    private fun showProgressOverlay() = (requireActivity() as BaseActivity).showProgressOverlay()

    private fun hideProgressOverlay() = (requireActivity() as BaseActivity).hideProgressOverlay()

    private fun updateProgress(text: String, currentFileIdx: Int?, totalFileCount: Int?) =
        (requireActivity() as BaseActivity).updateProgress(text, currentFileIdx, totalFileCount)

}