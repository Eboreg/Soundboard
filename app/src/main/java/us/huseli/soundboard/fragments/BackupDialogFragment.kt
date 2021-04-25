package us.huseli.soundboard.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import us.huseli.soundboard.R
import us.huseli.soundboard.activities.BaseActivity
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Settings
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupDialogFragment : DialogFragment() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private lateinit var dialog: AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.backup_settings_and_data)
            .setMessage(R.string.export_description)
            .setPositiveButton(R.string.do_it, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onBackupConfirmClick() }
        }
        return dialog
    }

    private fun onBackupConfirmClick() {
        dialog.hide()
        showProgressOverlay()
        scope.launch {
            val resultFragment = StatusDialogFragment().setTitle(getString(R.string.backup_results))
            try {
                tryBackup()
                resultFragment.addMessage(
                    StatusDialogFragment.Status.SUCCESS, getString(R.string.backup_successful))
            } catch (e: Exception) {
                resultFragment.addException(e)
                resultFragment.addMessage(StatusDialogFragment.Status.ERROR, getString(R.string.backup_failed))
            }
            hideProgressOverlay()
            dismiss()
            (requireActivity() as BaseActivity).showFragment(resultFragment)
        }
    }

    private fun tryBackup() {
        val json = Settings.fromPrefs(PreferenceManager.getDefaultSharedPreferences(requireContext())).toJson()
        val jsonFile = File(
            requireActivity().getDir(Constants.BACKUP_TEMP_DIRNAME, Context.MODE_PRIVATE),
            Constants.ZIP_PREFS_FILENAME)
        jsonFile.writeText(json)

        val dbFile = requireActivity().getDatabasePath(Constants.DATABASE_NAME)
        val soundFiles = requireActivity().getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE).listFiles()
        val outDir = File(requireActivity().filesDir, "backups")
        outDir.mkdir()

        val date = DateFormat.format("yyyy-MM-dd", Date()).toString()
        val outFile = File(outDir, "soundboard-$date.zip")

        try {
            val totalFileCount =
                requireActivity().getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE)?.listFiles()?.size?.plus(2)
            var currentFileIdx = 0

            ZipOutputStream(FileOutputStream(outFile)).use { zipOut ->
                updateProgress(getString(R.string.backing_up_settings), currentFileIdx++, totalFileCount)
                addFileToZip("", jsonFile, zipOut)
                updateProgress(getString(R.string.backing_up_database), currentFileIdx++, totalFileCount)
                addFileToZip(Constants.ZIP_DB_DIR, dbFile, zipOut)
                soundFiles?.forEach {
                    updateProgress(getString(R.string.backing_up_sounds), currentFileIdx++, totalFileCount)
                    addFileToZip(Constants.ZIP_SOUNDS_DIR, it, zipOut)
                }
            }

            requireActivity()
                .getDir(Constants.BACKUP_TEMP_DIRNAME, Context.MODE_PRIVATE)
                .listFiles()
                ?.forEach { it.delete() }

            val uri = FileProvider.getUriForFile(requireContext(), "us.huseli.soundboard.fileprovider", outFile)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, requireActivity().contentResolver.getType(uri))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_with)))
        } finally {
            outFile.deleteOnExit()
        }
    }

    private fun addFileToZip(basePath: String, file: File, zipOut: ZipOutputStream) {
        /** Should be run on IO background thread */
        val data = ByteArray(Constants.ZIP_BUFFER_SIZE)
        val outPath = basePath.trimEnd('/') + "/" + file.path.substring(file.path.lastIndexOf("/") + 1)
        zipOut.putNextEntry(ZipEntry(outPath))
        BufferedInputStream(FileInputStream(file), Constants.ZIP_BUFFER_SIZE).use { inputStream ->
            do {
                val count = inputStream.read(data, 0, Constants.ZIP_BUFFER_SIZE)
                if (count != -1) zipOut.write(data, 0, count)
            } while (count != -1)
        }
        zipOut.closeEntry()
    }

    private fun showProgressOverlay() = (requireActivity() as BaseActivity).showProgressOverlay()

    private fun hideProgressOverlay() = (requireActivity() as BaseActivity).hideProgressOverlay()

    private fun updateProgress(text: String, currentFileIdx: Int?, totalFileCount: Int?) =
        (requireActivity() as BaseActivity).updateProgress(text, currentFileIdx, totalFileCount)

}