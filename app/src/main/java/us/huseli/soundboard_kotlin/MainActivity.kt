package us.huseli.soundboard_kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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
import androidx.fragment.app.Fragment
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.actionbar.*
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ActivityMainBinding
import us.huseli.soundboard_kotlin.fragments.*
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundAddMultipleViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundAddViewModel

class MainActivity :
        AppCompatActivity(),
        EditSoundInterface,
        EditCategoryInterface,
        AppViewModelListenerInterface,
        ColorPickerDialogListener {
    private var categories = emptyList<Category>()

    private val preferences: SharedPreferences by lazy { getPreferences(Context.MODE_PRIVATE) }
    private val categoryListViewModel by viewModels<CategoryListViewModel>()
    private val appViewModel by viewModels<AppViewModel>()
    private val soundAddViewModel by viewModels<SoundAddViewModel>()
    private val soundAddMultipleViewModel by viewModels<SoundAddMultipleViewModel>()
    private var toast: Toast? = null

    private lateinit var binding: ActivityMainBinding

    // These are just to know whether a toast should be shown on value change
    private var zoomLevel: Int? = null

    private var reorderEnabled: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
        }

        appViewModel.zoomLevel.observe(this, { value -> onZoomLevelChange(value) })
        preferences.getInt("zoomLevel", 0).let { if (it != 0) appViewModel.setZoomLevel(it) }

        // Keep track of these to be able to send categoryIndex to EditSoundDialogFragment
        categoryListViewModel.categories.observe(this, {
            categories = it
            if (categories.isEmpty()) categoryListViewModel.create(getString(R.string.default_category))
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_SOUND_GET)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // We have returned from file chooser dialog
        if (requestCode == REQUEST_SOUND_GET && resultCode == Activity.RESULT_OK && data != null) {
            if (data.clipData != null) {
                // Multiple items selected; from data.clipData we get Uri:s
                data.clipData?.let { clipData ->
                    val sounds = mutableListOf<Sound>()
                    for (i in 0 until clipData.itemCount) {
                        sounds.add(makeSoundFromUri(clipData.getItemAt(i).uri, data.flags))
                    }
                    soundAddMultipleViewModel.setup(sounds, getString(R.string.multiple_sounds_selected))
                    showMultipleSoundAddDialog()
                }
            } else {
                // One item selected; data.data is a Uri
                data.data?.let { uri ->
                    val sound = makeSoundFromUri(uri, data.flags)
                    soundAddViewModel.setup(sound)
                    showSoundAddDialog()
                }
            }
        }
    }

    private fun showMultipleSoundAddDialog() = showDialogFragment(AddMultipleSoundDialogFragment(), null)

    private fun makeSoundFromUri(uri: Uri, flags: Int): Sound {
        val soundName: String
        // FLAG_GRANT_READ_URI_PERMISSION is not one of the permissions we are requesting
        // here, so bitwise-AND it away
        contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
        return Sound(soundName, uri)
    }

    private fun showDialogFragment(fragment: Fragment, tag: String?) {
        supportFragmentManager.beginTransaction().apply {
            add(fragment, tag)
            show(fragment)
            commit()
        }
    }

    override fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int) {
        showDialogFragment(DeleteCategoryFragment.newInstance(id, name, soundCount), null)
    }

    override fun showCategoryAddDialog() {
        showDialogFragment(AddCategoryDialogFragment.newInstance(DIALOG_TAGS.indexOf(CATEGORY_ADD_DIALOG_TAG)), CATEGORY_ADD_DIALOG_TAG)
    }

    override fun showCategoryEditDialog(categoryId: Int) {
        showDialogFragment(EditCategoryDialogFragment.newInstance(categoryId, DIALOG_TAGS.indexOf(CATEGORY_EDIT_DIALOG_TAG)), CATEGORY_EDIT_DIALOG_TAG)
    }

    private fun showSoundAddDialog() = showDialogFragment(AddSoundDialogFragment(), null)

    override fun showSoundEditDialog(soundId: Int, categoryId: Int?) {
        var categoryIndex = categories.map { it.id }.indexOf(categoryId)
        if (categoryIndex == -1) categoryIndex = 0
        showDialogFragment(EditSoundDialogFragment.newInstance(soundId, categoryIndex), null)
    }

    override fun showSoundDeleteDialog(soundId: Int, soundName: String?) {
        showDialogFragment(DeleteSoundFragment.newInstance(soundId, soundName ?: ""), null)
    }

    override fun onReorderEnabledChange(value: Boolean) {
        val item = toolbar.menu.findItem(R.id.action_toggle_reorder)
        if (value) {
            if (reorderEnabled != null) showToast(R.string.reordering_enabled)
            item.icon.alpha = 255
        } else {
            if (reorderEnabled != null) showToast(R.string.reordering_disabled)
            item.icon.alpha = 127
        }
        reorderEnabled = value
    }

    override fun onZoomLevelChange(value: Int) {
        if (zoomLevel != null) showToast(getString(R.string.zoom_level_colon) + value)
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

    private fun showToast(text: CharSequence) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }
    }

    private fun showToast(textResource: Int) = showToast(getText(textResource))

    companion object {
        const val REQUEST_SOUND_GET = 1
        const val CATEGORY_ADD_DIALOG_TAG = "categoryAddDialog"
        const val CATEGORY_EDIT_DIALOG_TAG = "categoryEditDialog"
        val DIALOG_TAGS = listOf(CATEGORY_ADD_DIALOG_TAG, CATEGORY_EDIT_DIALOG_TAG)
    }
}