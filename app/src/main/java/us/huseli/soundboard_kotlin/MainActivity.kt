package us.huseli.soundboard_kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.CategoryListViewModel
import us.huseli.soundboard_kotlin.data.SoundListViewModel
import us.huseli.soundboard_kotlin.data.SoundViewModel
import us.huseli.soundboard_kotlin.helpers.EditSoundCategoryInterface
import us.huseli.soundboard_kotlin.helpers.EditSoundInterface

class MainActivity : AppCompatActivity(), EditSoundInterface, EditSoundCategoryInterface {
    companion object {
        const val REQUEST_SOUND_GET = 1
    }

    var categories = emptyList<Category>()

    private lateinit var preferences: SharedPreferences
    private val viewModel by viewModels<SoundListViewModel>()
    private val categoryListViewModel by viewModels<CategoryListViewModel>()
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = getPreferences(Context.MODE_PRIVATE)
        viewModel.zoomLevel.value = preferences.getInt("zoomLevel", 0)
        viewModel.zoomLevel.observe(this, Observer {
            with(preferences.edit()) {
                putInt("zoomLevel", it)
                apply()
            }
        })
        categoryListViewModel.categories.observe(this, Observer { categories = it })
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        viewModel.reorderEnabled.observe(this, Observer { onReorderEnabledChange(it) })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_sound -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.type = "audio/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_SOUND_GET)
            }
            R.id.action_toggle_reorder -> {
                viewModel.reorderEnabled.value = !viewModel.reorderEnabled.value!!
                if (viewModel.reorderEnabled.value!!)
                    Toast.makeText(this, R.string.reordering_enabled, Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, R.string.reordering_disabled, Toast.LENGTH_SHORT).show()
            }
            R.id.action_zoom_in -> {
                viewModel.zoomLevel.value = viewModel.zoomLevel.value?.plus(1)
            }
            R.id.action_zoom_out -> {
                viewModel.zoomLevel.value = viewModel.zoomLevel.value?.minus(1)
            }
            R.id.action_add_category -> {
                showSoundCategoryEditDialog(null)
            }
        }
        return true
    }

    override fun onSoundCategoryDialogSave(category: Category) {
        categoryListViewModel.save(category)
    }

    override fun showSoundCategoryEditDialog(category: Category?) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditCategoryFragment()
            add(0, fragment)
            show(fragment)
            commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // We have returned from file chooser dialog and data.data is a URI
        if (requestCode == REQUEST_SOUND_GET && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val soundName: String
                when (val cursor = contentResolver.query(it, null, null, null, null)) {
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
                showSoundEditDialog(SoundViewModel.getInstance(application, soundName, it))
            }
        }
    }

    override fun showSoundEditDialog(soundViewModel: SoundViewModel) {
        supportFragmentManager.beginTransaction().apply {
            val fragment = EditSoundFragment.newInstance(soundViewModel, categories)
            add(0, fragment)
            show(fragment)
            commit()
        }
    }

    private fun onReorderEnabledChange(value: Boolean) {
        val item = toolbar.menu.findItem(R.id.action_toggle_reorder)
        if (value) item.icon.alpha = 255 else item.icon.alpha = 127
    }

    override fun onSoundDialogSave(soundViewModel: SoundViewModel) = soundViewModel.save()
}