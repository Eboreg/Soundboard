package us.huseli.soundboard.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.LevelListDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.R
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.PlayerRepository
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.ActivityMainBinding
import us.huseli.soundboard.fragments.*
import us.huseli.soundboard.helpers.Functions
import us.huseli.soundboard.helpers.SettingsManager
import us.huseli.soundboard.interfaces.EditCategoryInterface
import us.huseli.soundboard.interfaces.EditSoundInterface
import us.huseli.soundboard.interfaces.SnackbarInterface
import us.huseli.soundboard.interfaces.ZoomInterface
import us.huseli.soundboard.viewmodels.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity :
    BaseActivity(),
    EditCategoryInterface,
    EditSoundInterface,
    ColorPickerDialogListener,
    SnackbarInterface,
    ZoomInterface,
    ActionMode.Callback {

    @Inject
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var settingsManager: SettingsManager

    private val actionbarLogoTouchTimes = mutableListOf<Long>()
    private val appViewModel by viewModels<AppViewModel>()
    private val categoryEditViewModel by viewModels<CategoryEditViewModel>()
    private val categoryListViewModel by viewModels<CategoryViewModel>()
    private val soundAddViewModel by viewModels<SoundAddViewModel>()
    private val soundEditViewModel by viewModels<SoundEditViewModel>()
    private val soundViewModel by viewModels<SoundViewModel>()

    private var actionMode: ActionMode? = null
    private var addSoundLauncher: ActivityResultLauncher<Intent>? = null
    private var allSounds = emptyList<Sound>()  // used for soundAddViewModel
    private var categories = emptyList<Category>()
    private var filterEnabled: Boolean = false
    private var filterWasEnabled: Boolean = false
    private var reorderEnabled: Boolean? = null
    private var repressMode: SoundPlayer.RepressMode? = null

    private lateinit var binding: ActivityMainBinding

    /********* OVERRIDDEN ANDROID METHODS **********/
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        /**
         * TODO: Find a good way to shortcut from here directly to playing the sound at ev.x, ev.y
         * if there is one and it should be played.
         * We need some global object that stores x, y, height and width for every sound CardView,
         * as well as a boolean that says whether sounds should be played on click (if we are in
         * reorder mode, they should not).
         * These data must be updated when sound CardView positions change (could be due to
         * scrolling, category collapse/expand, sound add/delete/move etc.)
         * Specifically, there needs to be a method that takes arguments x and y (position of
         * click), knows which sound (if any) is in that position right now, and starts playing it
         * if sounds should be played.
         * Maybe this can be implemented in PlayerRepository, since it already keeps track of all
         * SoundPlayer objects. It only needs to also keep track of their view boundaries.
         */
        return super.dispatchTouchEvent(ev)
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<CategoryListFragment>(R.id.content_container)
            }
        }

        addSoundLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            onAddSoundResult(it.data, it.resultCode)
        }

        // Take care of any files shared with us by other apps
        when (intent?.action) {
            Intent.ACTION_SEND -> (intent?.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                addSoundsFromUris(listOf(it))
            }
            Intent.ACTION_SEND_MULTIPLE -> intent?.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
                addSoundsFromUris(it.filterIsInstance<Uri>())
            }
        }

        // If version is higher than on last run
        getAppVersion().also {
            if (settingsManager.getLastVersion() < it) onAppVersionUpgraded(settingsManager.getLastVersion(), it)
            settingsManager.setLastVersion(it)
        }

        // This method will be run anew if device is rotated
        appViewModel.setOrientation(resources.configuration.orientation)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.soundViewModel = soundViewModel
        setContentView(binding.root)

        setSupportActionBar(binding.actionbar.actionbarToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupEasterEggClickListener()
        setupBottomBar()

        binding.filterTerm.addTextChangedListener { soundViewModel.setFilterTerm(it) }
        binding.filterTerm.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    // Hide soft keyboard on "search" button press
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
                        binding.filterTerm.windowToken,
                        0
                    )
                    true
                }
                else -> false
            }
        }

        soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }

        soundViewModel.allSounds.observe(this) { allSounds = it }

        categoryListViewModel.categories.observe(this) {
            // Keep track of these to be able to send categoryIndex to EditSoundDialogFragment
            categories = it
            if (it.isEmpty()) categoryListViewModel.create(Functions.umlautify("Defäult"))
        }

        appViewModel.deleteOrphans(getDir(Constants.SOUND_DIRNAME, Context.MODE_PRIVATE))

        settingsManager.getWatchFolder()?.also { uri ->
            soundViewModel.syncWatchedFolder(this, uri, settingsManager.getWatchFolderTrashMissing())
        }
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
        if (resources.configuration.screenWidthDp >= 480) {
            menuInflater.inflate(R.menu.bottom_menu, menu)
            menu.findItem(R.id.action_open_repress_mode_menu)?.setIcon(
                R.drawable.repress_mode_menu_icon_with_down_caret
            )
        }
        // This has to be done here, because the callbacks require the menu to exist
        appViewModel.repressMode.observe(this) { onRepressModeChange(it) }
        appViewModel.zoomInPossible.observe(this) { onZoomInPossibleChange(it) }
        soundViewModel.filterEnabled.observe(this) { onFilterEnabledChange(it) }
        appViewModel.reorderEnabled.observe(this) { onReorderEnabledChange(it) }
        appViewModel.isUndoPossible.observe(this) { onUndoPossibleChange(it) }
        appViewModel.isRedoPossible.observe(this) { onRedoPossibleChange(it) }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) = soundViewModel.disableSelect()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_category -> showCategoryAddDialog()
            R.id.action_add_sound -> startAddSoundActivity()
            R.id.action_set_repress_mode_stop -> appViewModel.setRepressMode(SoundPlayer.RepressMode.STOP)
            R.id.action_set_repress_mode_restart -> appViewModel.setRepressMode(SoundPlayer.RepressMode.RESTART)
            R.id.action_set_repress_mode_overlap -> appViewModel.setRepressMode(SoundPlayer.RepressMode.OVERLAP)
            R.id.action_set_repress_mode_pause -> appViewModel.setRepressMode(SoundPlayer.RepressMode.PAUSE)
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
            R.id.action_help -> {
                supportFragmentManager.commit {
                    replace<HelpFragment>(R.id.content_container)
                    setReorderingAllowed(true)
                    addToBackStack("help")
                }
            }
            R.id.action_toggle_filter -> toggleFilterEnabled()
            R.id.action_toggle_reorder -> appViewModel.toggleReorderEnabled()
            R.id.action_undo -> undo()
            R.id.action_redo -> redo()
            R.id.action_zoom_in -> zoomIn()
            R.id.action_zoom_out -> zoomOut()
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false


    /********* OVERRIDDEN 3RD PARTY METHODS **********/
    override fun onColorSelected(dialogId: Int, color: Int) {
        // Have to do this just because ColorPickerDialog won't accept a Fragment as context :/
        (supportFragmentManager.findFragmentByTag(DIALOG_TAGS[dialogId]) as ColorPickerDialogListener)
            .onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit


    /********* OVERRIDDEN OWN METHODS **********/
    override fun showCategoryAddDialog() {
        showFragment(
            AddCategoryDialogFragment.newInstance(DIALOG_TAGS.indexOf(CATEGORY_ADD_DIALOG_TAG)),
            CATEGORY_ADD_DIALOG_TAG
        )
    }

    override fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int) =
        showFragment(DeleteCategoryFragment.newInstance(id, name, soundCount, categories.size))

    override fun showCategoryEditDialog(category: Category) {
        categoryEditViewModel.setup(category)
        showFragment(
            EditCategoryDialogFragment.newInstance(DIALOG_TAGS.indexOf(CATEGORY_EDIT_DIALOG_TAG)),
            CATEGORY_EDIT_DIALOG_TAG
        )
    }

    override fun showSnackbar(text: CharSequence) =
        Snackbar.make(binding.appCoordinator, Functions.umlautify(text), Snackbar.LENGTH_SHORT).show()

    override fun showSnackbar(textResource: Int) = showSnackbar(getText(textResource))

    override fun showSoundAddDialog() = showFragment(AddSoundDialogFragment())

    override fun showSoundDeleteDialog(sounds: List<Sound>) {
        val validSounds = sounds.filter { it.id != null }
        when {
            validSounds.size == 1 -> {
                val sound = validSounds.first()
                sound.id?.let { soundId -> showFragment(DeleteSoundFragment.newInstance(soundId, sound.name)) }
            }
            validSounds.size > 1 -> showFragment(DeleteSoundFragment.newInstance(sounds.map { it.id }))
        }
    }

    override fun showSoundEditDialog(sound: Sound) = showSoundEditDialog(listOf(sound))

    override fun showSoundEditDialog(sounds: List<Sound>) {
        soundEditViewModel.setup(sounds, getString(R.string.multiple_sounds_selected, sounds.size))
        val categoryIndex = getCategoryIndex(sounds)
        showFragment(EditSoundDialogFragment.newInstance(categoryIndex))
    }

    override fun zoomIn() = appViewModel.zoomIn()?.let { showSnackbar(getString(R.string.zoom_level_percent, it)) }

    override fun zoomOut() = appViewModel.zoomOut()?.let { showSnackbar(getString(R.string.zoom_level_percent, it)) }


    /********* OWN METHODS **********/
    private fun addSoundsFromUris(uris: List<Uri>) {
        /** Used when adding sounds from within app and sharing sounds from other apps */
        val sounds = mutableListOf<Sound>()
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

        soundAddViewModel.setup(
            sounds,
            this.allSounds,
            getString(R.string.multiple_sounds_selected, sounds.size)
        )

        when {
            soundAddViewModel.hasDuplicates -> showFragment(AddDuplicateSoundDialogFragment())
            sounds.isEmpty() -> showSnackbar(R.string.no_sounds_to_add)
            else -> showFragment(AddSoundDialogFragment())
        }
    }

    private fun getCategoryIndex(selectedSounds: List<Sound>): Int {
        return when (selectedSounds.size) {
            1 -> categories.map { it.id }.indexOf(selectedSounds.first().categoryId).let { if (it > -1) it else 0 }
            else -> 0
        }
    }

    private fun onAddSoundResult(data: Intent?, resultCode: Int) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            // We have returned from file chooser dialog
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

            addSoundsFromUris(uris)
        } else if (resultCode == Activity.RESULT_CANCELED) showSnackbar(R.string.no_sound_chosen)
    }

    private fun onAppVersionUpgraded(from: Long, to: Long) {
        if (from in 1..5 && to >= 6) {
            /** From 6, sounds are stored in app local storage */
            try {
                soundViewModel.moveFilesToLocalStorage(applicationContext)
            } catch (e: Exception) {
                val fragment = StatusDialogFragment()
                    .setTitle(getString(R.string.an_error_occurred))
                    .addMessage(
                        StatusDialogFragment.Status.ERROR,
                        getString(R.string.fail_move_sounds_local)
                    )
                    .addException(e)
                showFragment(fragment)
            }
        }
        if (from in 1..6 && to >= 7) {
            /** From 7, checksums for sounds are stored */
            soundViewModel.saveChecksums()
        }
    }

    private fun onFilterEnabledChange(value: Boolean) {
        filterEnabled = value
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (value) {
            binding.filterBar.visibility = View.VISIBLE
            binding.filterTerm.requestFocus()
            manager?.showSoftInput(binding.filterTerm, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.filterBar.visibility = View.GONE
            manager?.hideSoftInputFromWindow(binding.filterTerm.windowToken, 0)
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
        val level = when (mode) {
            SoundPlayer.RepressMode.OVERLAP -> resources.getInteger(R.integer.repress_mode_overlap)
            SoundPlayer.RepressMode.RESTART -> resources.getInteger(R.integer.repress_mode_restart)
            SoundPlayer.RepressMode.STOP -> resources.getInteger(R.integer.repress_mode_stop)
            SoundPlayer.RepressMode.PAUSE -> resources.getInteger(R.integer.repress_mode_pause)
        }
        val menuItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_open_repress_mode_menu)
            ?: binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_open_repress_mode_menu)
        val icon = (menuItem?.icon as? LayerDrawable)?.getDrawable(0) as? LevelListDrawable
        icon?.level = level
        if (repressMode != null) showSnackbar(getString(R.string.on_repress, mode))
        repressMode = mode
        settingsManager.setRepressMode(mode)
    }

    private fun onSelectEnabledChange(value: Boolean) {
        actionMode = if (value) startSupportActionMode(this)
        else {
            actionMode?.finish()
            null
        }
    }

    private fun onRedoPossibleChange(value: Boolean?) {
        val redoItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_redo)
            ?: binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_redo)
        when (value) {
            true -> {
                redoItem?.isEnabled = true
                redoItem?.icon?.alpha = 255
            }
            else -> {
                redoItem?.isEnabled = false
                redoItem?.icon?.alpha = 128
            }
        }
    }

    private fun onUndoPossibleChange(value: Boolean?) {
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
        val zoomInItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_zoom_in)
            ?: binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_zoom_in)
        zoomInItem?.apply {
            isEnabled = value
            icon?.alpha = if (value) 255 else 128
        }
    }

    private fun setupBottomBar() {
        binding.bottombar.bottombarToolbar?.apply {
            setOnMenuItemClickListener { onOptionsItemSelected(it) }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEasterEggClickListener() {
        binding.actionbar.actionbarLogo.isClickable = true
        binding.actionbar.actionbarLogo.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) actionbarLogoTouchTimes.add(event.eventTime)
            if (actionbarLogoTouchTimes.size == 3) {
                if (actionbarLogoTouchTimes.first() + 1000 >= event.eventTime)
                    showFragment(EasterEggFragment())
                actionbarLogoTouchTimes.clear()
            } else if (actionbarLogoTouchTimes.size > 3)
                actionbarLogoTouchTimes.clear()
            true
        }
    }

    private fun startAddSoundActivity() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        if (intent.resolveActivity(packageManager) != null) addSoundLauncher?.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun toggleFilterEnabled() {
        if (reorderEnabled == true) showSnackbar(R.string.cannot_enable_filter)
        else soundViewModel.toggleFilterEnabled()
    }

    private fun redo() {
        appViewModel.redo()
        showSnackbar(R.string.redid)
    }

    private fun undo() {
        appViewModel.undo()
        showSnackbar(R.string.undid)
    }


    companion object {
        // const val EXTRA_SOUND_ID = "soundId"
        const val CATEGORY_ADD_DIALOG_TAG = "categoryAddDialog"
        const val CATEGORY_EDIT_DIALOG_TAG = "categoryEditDialog"
        val DIALOG_TAGS = listOf(CATEGORY_ADD_DIALOG_TAG, CATEGORY_EDIT_DIALOG_TAG)
    }
}