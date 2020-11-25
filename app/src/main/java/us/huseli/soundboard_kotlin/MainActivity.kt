package us.huseli.soundboard_kotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ActivityMainBinding
import us.huseli.soundboard_kotlin.fragments.*
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.interfaces.ToastInterface
import us.huseli.soundboard_kotlin.interfaces.ZoomInterface
import us.huseli.soundboard_kotlin.viewmodels.*

class MainActivity :
        AppCompatActivity(),
        EditCategoryInterface,
        AppViewModelListenerInterface,
        ColorPickerDialogListener,
        ToastInterface,
        ZoomInterface,
        ActionMode.Callback {
    private val categoryListViewModel by viewModels<CategoryListViewModel>()
    private val appViewModel by viewModels<AppViewModel>()
    private val soundViewModel by viewModels<SoundViewModel>()
    private val soundAddViewModel by viewModels<SoundAddViewModel>()
    private val soundAddMultipleViewModel by viewModels<SoundAddMultipleViewModel>()
    private val soundEditMultipleViewModel by viewModels<SoundEditMultipleViewModel>()

    private var actionMode: ActionMode? = null
    private lateinit var binding: ActivityMainBinding
    private var categories = emptyList<Category>()

    // Just to know whether a toast should be shown on value change
    private var reorderEnabled: Boolean? = null
    private var toast: Toast? = null


    /** Overridden Android methods */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        // setContentView(R.layout.activity_main)
        setContentView(binding.root)

        setSupportActionBar(binding.actionbar.actionbarToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.actionbar.actionbarLogo.isClickable = true
        binding.actionbar.actionbarLogo.setOnClickListener { showEasterEgg() }

        soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }

        // Keep track of these to be able to send categoryIndex to EditSoundDialogFragment
        categoryListViewModel.categories.observe(this) {
            categories = it
            if (it.isEmpty()) categoryListViewModel.create(getString(R.string.default_category))
        }
    }

    private fun showEasterEgg() {
        showDialogFragment(EasterEggFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        // This has to be done here, because the callback requires the menu to exist
        soundViewModel.reorderEnabled.observe(this) { onReorderEnabledChange(it) }
        appViewModel.zoomInPossible.observe(this) { onZoomInPossibleChange(it) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_sound -> startAddSoundActivity()
            R.id.action_toggle_reorder -> soundViewModel.toggleReorderEnabled()
            R.id.action_zoom_in -> zoomIn()
            R.id.action_zoom_out -> zoomOut()
            R.id.action_add_category -> showDialogFragment(
                    AddCategoryDialogFragment.newInstance(DIALOG_TAGS.indexOf(CATEGORY_ADD_DIALOG_TAG)), CATEGORY_ADD_DIALOG_TAG)
            // R.id.action_reinit_failed_sounds -> reinitFailedSounds()
        }
        return true
    }

    @Suppress("unused")
    @SuppressLint("QueryPermissionsNeeded")
    private fun reinitFailedSounds() {
        // TODO: Generalize this shit somehow (with ordinary open file stuff)
        // TODO: Take up work on this stuff
        soundViewModel.failedSounds.forEach { sound ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, sound.uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            else
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "audio/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            intent.putExtra(EXTRA_SOUND_ID, sound.id)
            if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_SOUND_REINIT)
        }
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
                        sounds.add(Sound(clipData.getItemAt(i).uri, data.flags, contentResolver))
                    }
                    soundAddMultipleViewModel.setup(sounds, getString(R.string.multiple_sounds_selected))
                    showDialogFragment(AddMultipleSoundDialogFragment())
                }
            } else {
                // One item selected; data.data is a Uri
                data.data?.let { uri ->
                    val sound = Sound(uri, data.flags, contentResolver)
                    soundAddViewModel.setup(sound)
                    showDialogFragment(AddSoundDialogFragment())
                }
            }
        }
        // We have returned from failed sound reinit dialog
        else if (requestCode == REQUEST_SOUND_REINIT && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                (data.extras?.get(EXTRA_SOUND_ID) as? Int)?.let { soundId ->
                    // soundId = id of sound to replace
                    val sound = Sound(uri, data.flags, contentResolver)
                    soundViewModel.replaceSound(soundId, sound, this)
                }
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.actionmode_menu, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_all_sounds -> {
                soundViewModel.selectAll()
                true
            }
            R.id.edit_sounds -> {
                when (soundViewModel.selectedSounds.size) {
                    0 -> {
                    }
                    1 -> {
                        val sound = soundViewModel.selectedSounds.first()
                        var categoryIndex = categories.map { it.id }.indexOf(sound.categoryId)
                        if (categoryIndex == -1) categoryIndex = 0
                        sound.id?.let { soundId ->
                            showDialogFragment(EditSoundDialogFragment.newInstance(soundId, categoryIndex))
                        }
                    }
                    else -> {
                        soundEditMultipleViewModel.setup(
                                soundViewModel.selectedSounds.mapNotNull { it.id }, getString(R.string.multiple_sounds_selected))
                        showDialogFragment(EditMultipleSoundDialogFragment())
                    }
                }
                true
            }
            R.id.delete_sounds -> {
                when (soundViewModel.selectedSounds.size) {
                    0 -> {
                    }
                    1 -> {
                        val sound = soundViewModel.selectedSounds.first()
                        sound.id?.let { soundId ->
                            showDialogFragment(DeleteSoundFragment.newInstance(soundId, sound.name))
                        }
                    }
                    else -> showDialogFragment(DeleteSoundFragment.newInstance(soundViewModel.selectedSounds.map { it.id }))
                }
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) = soundViewModel.disableSelect()


    /** Overridden 3rd party methods */
    override fun onColorSelected(dialogId: Int, color: Int) {
        // Have to do this just because ColorPickerDialog won't accept a Fragment as context :/
        (supportFragmentManager.findFragmentByTag(DIALOG_TAGS[dialogId]) as ColorPickerDialogListener).onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit


    /** Overridden own methods */
    override fun onReorderEnabledChange(value: Boolean) {
        val item = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_toggle_reorder)
        if (value) {
            if (reorderEnabled != null) showToast(R.string.reordering_enabled)
            item?.icon?.alpha = 204
        } else {
            if (reorderEnabled != null) showToast(R.string.reordering_disabled)
            item?.icon?.alpha = 102
        }
        reorderEnabled = value
    }

    override fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int) =
            showDialogFragment(DeleteCategoryFragment.newInstance(id, name, soundCount))

    override fun showCategoryEditDialog(categoryId: Int) =
            showDialogFragment(EditCategoryDialogFragment.newInstance(categoryId, DIALOG_TAGS.indexOf(CATEGORY_EDIT_DIALOG_TAG)), CATEGORY_EDIT_DIALOG_TAG)

    override fun onSelectEnabledChange(value: Boolean) {
        actionMode = if (value)
            startSupportActionMode(this)
        else {
            actionMode?.finish()
            null
        }
    }

    override fun showToast(text: CharSequence) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }
    }

    override fun showToast(textResource: Int) = showToast(getText(textResource))

    override fun zoomOut() =
            appViewModel.zoomOut()?.let { showToast(getString(R.string.zoom_level_percent, it)) }

    override fun zoomIn() =
            appViewModel.zoomIn()?.let { showToast(getString(R.string.zoom_level_percent, it)) }


    /** Own methods */
    private fun onZoomInPossibleChange(value: Boolean) {
        val item = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_zoom_in)
        item?.isEnabled = value
        item?.icon?.alpha = if (value) 204 else 102
    }

    @SuppressLint("QueryPermissionsNeeded")
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

    private fun showDialogFragment(fragment: Fragment, tag: String?) {
        supportFragmentManager.beginTransaction().apply {
            add(fragment, tag)
            show(fragment)
            commit()
        }
    }

    private fun showDialogFragment(fragment: Fragment) = showDialogFragment(fragment, null)


    companion object {
        const val REQUEST_SOUND_GET = 1
        const val REQUEST_SOUND_REINIT = 2
        const val EXTRA_SOUND_ID = "soundId"
        const val CATEGORY_ADD_DIALOG_TAG = "categoryAddDialog"
        const val CATEGORY_EDIT_DIALOG_TAG = "categoryEditDialog"
        val DIALOG_TAGS = listOf(CATEGORY_ADD_DIALOG_TAG, CATEGORY_EDIT_DIALOG_TAG)
    }
}