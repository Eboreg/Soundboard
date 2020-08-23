package us.huseli.soundboard_kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.activity_main.*
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.fragments.*
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.viewmodels.*

class MainActivity : AppCompatActivity(), EditSoundInterface, EditCategoryInterface, AppViewModelListenerInterface, ColorPickerDialogListener {
    companion object {
        const val REQUEST_SOUND_GET = 1
        const val CATEGORY_ADD_DIALOG_TAG = "categoryAddDialog"
        const val CATEGORY_EDIT_DIALOG_TAG = "categoryEditDialog"
        val DIALOG_TAGS = listOf(CATEGORY_ADD_DIALOG_TAG, CATEGORY_EDIT_DIALOG_TAG)
    }

    private var categoryViewModels = emptyList<CategoryViewModel>()
    private var soundViewModels = emptyList<SoundViewModel>()

    private val preferences: SharedPreferences by lazy { getPreferences(Context.MODE_PRIVATE) }
    private val categoryListViewModel by viewModels<CategoryListViewModel>()
    private val soundListViewModel by viewModels<SoundListViewModel>()
    private val appViewModel by viewModels<AppViewModel>()

    // These are just to know whether a toast should be shown on value change
    private var zoomLevel: Int? = null
    private var reorderEnabled: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        appViewModel.zoomLevel.observe(this, { value -> onZoomLevelChange(value) })
        preferences.getInt("zoomLevel", 0).let { if (it != 0) appViewModel.setZoomLevel(it) }

        categoryListViewModel.categoryViewModels.observe(this, {
            Log.i(GlobalApplication.LOG_TAG, "MainActivity: categoryListViewModel.categoryViewModels changed: $it")
            categoryViewModels = it
        })

        // We keep track of these for the sake of EditSoundDialogFragment
        soundListViewModel.soundViewModels.observe(this, {
            Log.i(GlobalApplication.LOG_TAG, "MainActivity: soundListViewModel.soundViewModels changed: $it")
            soundViewModels = it
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        // This has to be done here, because the callback requires the menu to exist
        appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_sound -> startAddSoundActivity()
            R.id.action_toggle_reorder -> appViewModel.toggleReorderEnabled()
            R.id.action_zoom_in -> appViewModel.zoomIn()
            R.id.action_zoom_out -> appViewModel.zoomOut()
            R.id.action_add_category -> showCategoryAddDialog()
        }
        return true
    }

    private fun startAddSoundActivity() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        else
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_SOUND_GET)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // We have returned from file chooser dialog and data.data is a URI
        if (requestCode == REQUEST_SOUND_GET && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val soundName: String
                // FLAG_GRANT_READ_URI_PERMISSION is not one of the permissions we are requesting
                // here, so bitwise-AND it away
                contentResolver.takePersistableUriPermission(uri, data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
                when (val cursor = contentResolver.query(uri, null, null, null, null)) {
                    null -> soundName = ""
                    else -> {
                        cursor.moveToFirst()
                        var filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        if (filename.contains("."))
                            filename = filename.substring(0, filename.lastIndexOf("."))
                        soundName = filename
                        cursor.close()
                    }
                }
                showSoundAddDialog(Sound(soundName, uri))
            }
        }
    }

    override fun showCategoryDeleteDialog(categoryId: Int) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = DeleteCategoryFragment.newInstance(categoryId)
            add(0, fragment)
            show(fragment)
            commit()
        }
    }

    override fun showCategoryAddDialog() {
        // If categories exist, set category.order to max order + 1; else 0
        val lastCat = categoryListViewModel.categoryViewModels.value?.maxByOrNull { it.order }
        val order = lastCat?.order?.plus(1) ?: 0
        supportFragmentManager.beginTransaction().apply {
            val fragment = AddCategoryDialogFragment.newInstance(order, DIALOG_TAGS.indexOf(CATEGORY_ADD_DIALOG_TAG))
            add(fragment, CATEGORY_ADD_DIALOG_TAG)
            show(fragment)
            commit()
        }
    }

    override fun showCategoryEditDialog(categoryId: Int) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditCategoryDialogFragment.newInstance(categoryId, DIALOG_TAGS.indexOf(CATEGORY_EDIT_DIALOG_TAG))
            add(fragment, CATEGORY_EDIT_DIALOG_TAG)
            show(fragment)
            commit()
        }
    }

    private fun showSoundAddDialog(sound: Sound) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = AddSoundDialogFragment(sound)
            add(fragment, null)
            show(fragment)
            commit()
        }
    }

    override fun showSoundEditDialog(soundId: Int, categoryId: Int) {
        val categoryIndex = categoryViewModels.map { it.id }.indexOf(categoryId)
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditSoundDialogFragment.newInstance(soundId, categoryIndex)
            add(fragment, null)
            show(fragment)
            commit()
        }
    }

    override fun showSoundDeleteDialog(soundId: Int, soundName: String) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = DeleteSoundFragment.newInstance(soundId, soundName)
            add(fragment, null)
            show(fragment)
            commit()
        }
    }

    override fun onReorderEnabledChange(value: Boolean) {
        val item = toolbar.menu.findItem(R.id.action_toggle_reorder)
        if (value) {
            if (reorderEnabled != null) Toast.makeText(this, R.string.reordering_enabled, Toast.LENGTH_SHORT).show()
            item.icon.alpha = 255
        } else {
            if (reorderEnabled != null) Toast.makeText(this, R.string.reordering_disabled, Toast.LENGTH_SHORT).show()
            item.icon.alpha = 127
        }
        reorderEnabled = value
    }

    override fun onZoomLevelChange(value: Int) {
        if (zoomLevel != null) Toast.makeText(this, getString(R.string.zoom_level_colon) + value, Toast.LENGTH_SHORT).show()
        zoomLevel = value
        preferences.edit {
            putInt("zoomLevel", value)
            apply()
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        // Have to do this just because ColorPickerDialog won't accept a Fragment as context :/
        (supportFragmentManager.findFragmentByTag(DIALOG_TAGS[dialogId]) as ColorPickerDialogListener).onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit
}