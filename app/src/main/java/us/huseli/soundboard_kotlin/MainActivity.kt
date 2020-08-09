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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.lifecycle.Observer
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.fragments.AddSoundDialogFragment
import us.huseli.soundboard_kotlin.fragments.DeleteCategoryFragment
import us.huseli.soundboard_kotlin.fragments.EditCategoryDialogFragment
import us.huseli.soundboard_kotlin.fragments.EditSoundDialogFragment
import us.huseli.soundboard_kotlin.helpers.EditCategoryInterface
import us.huseli.soundboard_kotlin.helpers.EditSoundInterface
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel

class MainActivity : AppCompatActivity(), EditSoundInterface, EditCategoryInterface, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val REQUEST_SOUND_GET = 1
    }

    var categoryViewModels = emptyList<CategoryViewModel>()
    var soundViewModels = emptyList<SoundViewModel>()

    private val preferences: SharedPreferences by lazy { getPreferences(Context.MODE_PRIVATE) }
    private val categoryListViewModel by viewModels<CategoryListViewModel>()
    private val soundListViewModel by viewModels<SoundListViewModel>()
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        categoryListViewModel.categoryViewModels.observe(this, Observer { categoryViewModels = it })
        // We keep track of these for the sake of EditSoundDialogFragment
        soundListViewModel.soundViewModels.observe(this, Observer { soundViewModels = it })
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_sound -> {
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
            R.id.action_toggle_reorder -> {
                val value = !preferences.getBoolean("reorderEnabled", false)
                preferences.edit {
                    putBoolean("reorderEnabled", value)
                    apply()
                }
                if (value)
                    Toast.makeText(this, R.string.reordering_enabled, Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, R.string.reordering_disabled, Toast.LENGTH_SHORT).show()
            }
            R.id.action_zoom_in -> {
                val value = preferences.getInt("zoomLevel", 0) + 1
                preferences.edit {
                    putInt("zoomLevel", value)
                    apply()
                }
            }
            R.id.action_zoom_out -> {
                val value = preferences.getInt("zoomLevel", 0) - 1
                preferences.edit {
                    putInt("zoomLevel", value)
                    apply()
                }
            }
            R.id.action_add_category -> showCategoryEditDialog(null)
        }
        return true
    }

    override fun onCategoryDialogSave(category: Category) {
        categoryListViewModel.save(category)
    }

    override fun showCategoryDeleteDialog(categoryId: Int) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = DeleteCategoryFragment.newInstance(categoryId)
            add(0, fragment)
            show(fragment)
            commit()
        }
    }

    override fun showCategoryEditDialog(categoryId: Int?) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditCategoryDialogFragment.newInstance(categoryId)
            add(0, fragment)
            show(fragment)
            commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // We have returned from file chooser dialog and data.data is a URI
        if (requestCode == REQUEST_SOUND_GET && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val soundName: String
                contentResolver.takePersistableUriPermission(uri, data.flags)
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

    private fun showSoundAddDialog(sound: Sound) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = AddSoundDialogFragment(sound)
            add(fragment, null)
            show(fragment)
            commit()
        }
    }

    override fun showSoundEditDialog(soundId: Int) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditSoundDialogFragment.newInstance(soundId)
            add(fragment, null)
            show(fragment)
            commit()
        }
    }

    private fun onReorderEnabledChange(value: Boolean) {
        val item = toolbar.menu.findItem(R.id.action_toggle_reorder)
        if (value) item.icon.alpha = 255 else item.icon.alpha = 127
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences != null) {
            when (key) {
                "reorderEnabled" -> {
                    onReorderEnabledChange(sharedPreferences.getBoolean("reorderEnabled", false))
                }
            }
        }
    }

    override fun onStart() {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onResume")
        super.onResume()
    }

    override fun onStop() {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onPause")
        super.onPause()
    }

    override fun onRestart() {
        Log.i(GlobalApplication.LOG_TAG, "MainActivity ${this.hashCode()} onRestart")
        super.onRestart()
    }
}