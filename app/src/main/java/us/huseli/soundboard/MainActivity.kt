package us.huseli.soundboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.ActivityMainBinding
import us.huseli.soundboard.fragments.*
import us.huseli.soundboard.interfaces.*
import us.huseli.soundboard.viewmodels.*
import java.util.*

@AndroidEntryPoint
class MainActivity :
        LocaleActivity(),
        EditCategoryInterface,
        EditSoundInterface,
        ColorPickerDialogListener,
        SnackbarInterface,
        ZoomInterface,
        ActionMode.Callback,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private val categoryListViewModel by viewModels<CategoryViewModel>()
    private val appViewModel by viewModels<AppViewModel>()
    private val soundViewModel by viewModels<SoundViewModel>()
    private val soundAddViewModel by viewModels<SoundAddViewModel>()
    private val soundEditMultipleViewModel by viewModels<SoundEditViewModel>()

    private val actionbarLogoTouchTimes = mutableListOf<Long>()
    private var actionMode: ActionMode? = null
    private val addSoundLauncher = registerForActivityResult(StartActivityForResult()) { onAddSoundResult(it.data, it.resultCode) }
    private val reinitSoundsLauncher = registerForActivityResult(StartActivityForResult()) { onReInitSoundResult(it.data, it.resultCode) }
    private var categories = emptyList<Category>()
    private var filterEnabled: Boolean = false
    private var filterWasEnabled: Boolean = false
    private var reorderEnabled: Boolean? = null
    private var repressMode: SoundPlayer.RepressMode? = null
    private var sounds = emptyList<Sound>()  // used for soundAddViewModel

    private lateinit var binding: ActivityMainBinding

    /********* OVERRIDDEN ANDROID METHODS **********/
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_all_sounds -> {
                soundViewModel.selectAll()
                true
            }
            R.id.edit_sounds -> {
                showSoundEditDialog(soundViewModel.selectedSounds)
                true
            }
            R.id.delete_sounds -> {
                showSoundDeleteDialog(soundViewModel.selectedSounds)
                true
            }
            else -> false
        }
    }

    private fun onAddSoundResult(data: Intent?, resultCode: Int) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            // We have returned from file chooser dialog
            val sounds = mutableListOf<Sound>()
            val uris = mutableListOf<Uri>()

            data.clipData?.let { clipData ->
                // Multiple items selected; from data.clipData we get Uri:s
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: run {
                // Just one item selected
                data.data?.let { uris.add(it) }
            }

            uris.forEach { uri ->
                /**
                 * 1. Create Sound with original URI, don't copy any data
                 * 2. Send sounds to soundAddViewModel.setup()
                 * 3. Check for duplicates via Sound.checksum
                 * 4. In Add*SoundDialogFragment.save(), do copy data and save with new uri
                 */
                try {
                    sounds.add(Sound.createTemporary(uri, applicationContext))
                } catch (e: Exception) {
                    showSnackbar("Could not add ${uri.lastPathSegment}: $e")
                }
            }

            soundAddViewModel.setup(sounds, this.sounds, getString(R.string.multiple_sounds_selected, sounds.size))

            when {
                soundAddViewModel.hasDuplicates -> showDialogFragment(AddDuplicateSoundDialogFragment())
                sounds.isEmpty() -> showSnackbar(R.string.no_sounds_to_add)
                else -> showDialogFragment(AddSoundDialogFragment())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val lastVersion = prefs.getLong(PREF_LAST_VERSION, 0)

        @Suppress("DEPRECATION")
        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
        if (lastVersion < currentVersion) onAppVersionUpgraded(lastVersion, currentVersion)
        prefs.edit {
            putLong(PREF_LAST_VERSION, currentVersion)
            apply()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.soundViewModel = soundViewModel
        setContentView(binding.root)

        setSupportActionBar(binding.actionbar.actionbarToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupEasterEggClickListener()
        setupBottomBar()

        binding.filterTerm.addTextChangedListener {
            soundViewModel.setFilterTerm(it)
        }
        binding.filterTerm.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    // Hide soft keyboard on "search" button press
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(binding.filterTerm.windowToken, 0)
                    true
                }
                else -> false
            }
        }

        soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }

        soundViewModel.sounds.observe(this) { sounds = it }

        categoryListViewModel.categories.observe(this) {
            // Keep track of these to be able to send categoryIndex to EditSoundDialogFragment
            categories = it
            if (it.isEmpty()) categoryListViewModel.create(getString(R.string.default_category))
        }

        appViewModel.deleteOrphans(applicationContext)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        /** Inflates sound action menu when sounds are selected */
        mode.menuInflater.inflate(R.menu.actionmode_menu, menu)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /** Inflates top menu */
        menuInflater.inflate(R.menu.appbar_menu, menu)
        // On wider screens, also inflate "bottom menu" here
        if (resources.configuration.screenWidthDp >= 480) menuInflater.inflate(R.menu.bottom_menu, menu)
        // This has to be done here, because the callbacks require the menu to exist
        appViewModel.repressMode.observe(this) { onRepressModeChange(it) }
        appViewModel.zoomInPossible.observe(this) { onZoomInPossibleChange(it) }
        soundViewModel.filterEnabled.observe(this) { onFilterEnabledChange(it) }
        soundViewModel.reorderEnabled.observe(this) { onReorderEnabledChange(it) }
        appViewModel.undosAvailable.observe(this) { onUndosAvailableChange(it) }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) = soundViewModel.disableSelect()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_category -> showCategoryAddDialog()
            R.id.action_add_sound -> startAddSoundActivity()
            // R.id.action_reinit_failed_sounds -> reinitFailedSounds()
            R.id.action_set_repress_mode -> appViewModel.cycleRepressMode()
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            R.id.action_toggle_filter -> toggleFilterEnabled()
            R.id.action_toggle_reorder -> soundViewModel.toggleReorderEnabled()
            R.id.action_undo -> undo()
            R.id.action_zoom_in -> zoomIn()
            R.id.action_zoom_out -> zoomOut()
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.also {
            when (key) {
                "language" -> setLanguage(sharedPreferences.getString(key, "en"))
            }
        }
    }


    /********* OVERRIDDEN 3RD PARTY METHODS **********/
    override fun onColorSelected(dialogId: Int, color: Int) {
        // Have to do this just because ColorPickerDialog won't accept a Fragment as context :/
        (supportFragmentManager.findFragmentByTag(DIALOG_TAGS[dialogId]) as ColorPickerDialogListener).onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit


    /********* OVERRIDDEN OWN METHODS **********/
    override fun showCategoryAddDialog() =
            showDialogFragment(AddCategoryDialogFragment.newInstance(DIALOG_TAGS.indexOf(CATEGORY_ADD_DIALOG_TAG)), CATEGORY_ADD_DIALOG_TAG)

    override fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int) =
            showDialogFragment(DeleteCategoryFragment.newInstance(id, name, soundCount, categories.size))

    override fun showCategoryEditDialog(id: Int) =
            showDialogFragment(EditCategoryDialogFragment.newInstance(id, DIALOG_TAGS.indexOf(CATEGORY_EDIT_DIALOG_TAG)), CATEGORY_EDIT_DIALOG_TAG)

    override fun showCategorySortDialog(id: Int, name: String) =
            showDialogFragment(SortCategoryDialogFragment.newInstance(id, name))

    override fun showSnackbar(text: CharSequence) =
            Snackbar.make(binding.appCoordinator, text, Snackbar.LENGTH_SHORT).show()

    override fun showSnackbar(textResource: Int) = showSnackbar(getText(textResource))

    override fun showSoundAddDialog() = showDialogFragment(AddSoundDialogFragment())

    override fun showSoundDeleteDialog(sounds: List<Sound>) {
        val validSounds = sounds.filter { it.id != null }
        when {
            validSounds.size == 1 -> {
                val sound = validSounds.first()
                sound.id?.let { soundId ->
                    showDialogFragment(DeleteSoundFragment.newInstance(soundId, sound.name))
                }
            }
            validSounds.size > 1 -> showDialogFragment(DeleteSoundFragment.newInstance(sounds.map { it.id }))
        }
    }

    override fun showSoundEditDialog(sound: Sound) = showSoundEditDialog(listOf(sound))

    override fun showSoundEditDialog(sounds: List<Sound>) {
        soundEditMultipleViewModel.setup(sounds, getString(R.string.multiple_sounds_selected, sounds.size))
        val categoryIndex = getCategoryIndex(sounds)
        showDialogFragment(EditSoundDialogFragment.newInstance(categoryIndex))
    }

    override fun zoomIn() = appViewModel.zoomIn()?.let { showSnackbar(getString(R.string.zoom_level_percent, it)) }

    override fun zoomOut() = appViewModel.zoomOut()?.let { showSnackbar(getString(R.string.zoom_level_percent, it)) }


    /********* OWN METHODS **********/
    private fun getCategoryIndex(selectedSounds: List<Sound>): Int {
        return when (selectedSounds.size) {
            1 -> categories.map { it.id }.indexOf(selectedSounds.first().categoryId).let { if (it > -1) it else 0 }
            else -> 0
        }
    }

    private fun onAppVersionUpgraded(from: Long, to: Long) {
        if (from in 1..5 && to >= 6) {
            /** From 6, sounds are stored in app local storage */
            soundViewModel.moveFilesToLocalStorage(applicationContext)
        }
        if (from in 1..6 && to >= 7) {
            /** From 7, checksums for sounds are stored */
            soundViewModel.saveChecksums()
        }
    }

    private fun onFilterEnabledChange(value: Boolean) {
        filterEnabled = value
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val item = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_toggle_filter)
        if (value) {
            item?.icon?.alpha = 255
            binding.filterBar.visibility = View.VISIBLE
            binding.filterTerm.requestFocus()
            manager?.showSoftInput(binding.filterTerm, InputMethodManager.SHOW_IMPLICIT)
        } else {
            item?.icon?.alpha = 128
            binding.filterBar.visibility = View.GONE
            manager?.hideSoftInputFromWindow(binding.filterTerm.windowToken, 0)
        }
    }

    private fun onReInitSoundResult(data: Intent?, resultCode: Int) {
        // We have returned from failed sound reinit dialog
        if (resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                (data.extras?.get(EXTRA_SOUND_ID) as? Int)?.let { soundId ->
                    // soundId = id of sound to replace
                    val sound = Sound.createTemporary(uri, applicationContext)
                    soundViewModel.replaceSound(soundId, sound, this)
                }
            }
        }
    }

    private fun onReorderEnabledChange(value: Boolean) {
        val item = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_toggle_reorder)
        if (value) {
            if (reorderEnabled != null) showSnackbar(R.string.reordering_enabled)
            item?.icon?.alpha = 255
            filterWasEnabled = filterEnabled
            soundViewModel.disableFilter()
        } else {
            if (reorderEnabled != null) showSnackbar(R.string.reordering_disabled)
            item?.icon?.alpha = 128
            if (filterWasEnabled) soundViewModel.enableFilter()
        }
        reorderEnabled = value  // Has to come last
    }

    private fun onRepressModeChange(mode: SoundPlayer.RepressMode) {
        val icon = when (mode) {
            SoundPlayer.RepressMode.OVERLAP -> ResourcesCompat.getDrawable(resources, R.drawable.ic_repress_overlap, theme)
            SoundPlayer.RepressMode.RESTART -> ResourcesCompat.getDrawable(resources, R.drawable.ic_repress_restart, theme)
            SoundPlayer.RepressMode.STOP -> ResourcesCompat.getDrawable(resources, R.drawable.ic_repress_stop, theme)
        }
        binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_set_repress_mode)?.icon = icon
        binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_set_repress_mode)?.icon = icon
        if (this.repressMode != null) showSnackbar(getString(R.string.on_repress, mode))
        this.repressMode = mode
    }

    private fun onSelectEnabledChange(value: Boolean) {
        actionMode = if (value)
            startSupportActionMode(this)
        else {
            actionMode?.finish()
            null
        }
    }

    private fun onUndosAvailableChange(value: Boolean?) {
        // There are 1 or more undos in the queue
        val undoItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_undo)
                ?: binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_undo)
        when (value) {
            true -> {
                undoItem?.isEnabled = true
                undoItem?.icon?.alpha = 255
            }
            else -> {
                undoItem?.isEnabled = false
                undoItem?.icon?.alpha = 128
            }
        }
    }

    private fun onZoomInPossibleChange(value: Boolean) {
        binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_zoom_in)?.apply {
            isEnabled = value
            icon?.alpha = if (value) 255 else 128
        }
        binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_zoom_in)?.apply {
            isEnabled = value
            icon?.alpha = if (value) 255 else 128
        }
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
            if (intent.resolveActivity(packageManager) != null) reinitSoundsLauncher.launch(intent)
        }
    }

    private fun setupBottomBar() {
        binding.bottombar.bottombarToolbar?.setOnMenuItemClickListener { onOptionsItemSelected(it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEasterEggClickListener() {
        binding.actionbar.actionbarLogo.isClickable = true
        binding.actionbar.actionbarLogo.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) actionbarLogoTouchTimes.add(event.eventTime)
            if (actionbarLogoTouchTimes.size == 3) {
                if (actionbarLogoTouchTimes.first() + 1000 >= event.eventTime) showDialogFragment(EasterEggFragment())
                actionbarLogoTouchTimes.clear()
            } else if (actionbarLogoTouchTimes.size > 3)
                actionbarLogoTouchTimes.clear()
            true
        }
    }

    private fun showDialogFragment(fragment: Fragment, tag: String?) {
        supportFragmentManager
                .beginTransaction()
                .add(fragment, tag)
                .show(fragment)
                .commit()
    }

    private fun showDialogFragment(fragment: Fragment) = showDialogFragment(fragment, null)

    @SuppressLint("QueryPermissionsNeeded")
    private fun startAddSoundActivity() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        // if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_SOUND_ADD)
        if (intent.resolveActivity(packageManager) != null) addSoundLauncher.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun toggleFilterEnabled() {
        if (reorderEnabled == true) showSnackbar(R.string.cannot_enable_filter)
        else soundViewModel.toggleFilterEnabled()
    }

    private fun undo() {
        appViewModel.undo()
        showSnackbar(R.string.undid)
    }


    companion object {
        const val REQUEST_SOUND_ADD = 1
        const val REQUEST_SOUND_REINIT = 2
        const val EXTRA_SOUND_ID = "soundId"
        const val CATEGORY_ADD_DIALOG_TAG = "categoryAddDialog"
        const val CATEGORY_EDIT_DIALOG_TAG = "categoryEditDialog"
        const val LOG_TAG = "MainActity"
        const val PREF_LAST_VERSION = "lastRunVersionCode"
        val DIALOG_TAGS = listOf(CATEGORY_ADD_DIALOG_TAG, CATEGORY_EDIT_DIALOG_TAG)
    }
}