package us.huseli.soundboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.ActivityMainBinding
import us.huseli.soundboard.fragments.*
import us.huseli.soundboard.interfaces.EditCategoryInterface
import us.huseli.soundboard.interfaces.ToastInterface
import us.huseli.soundboard.interfaces.ZoomInterface
import us.huseli.soundboard.viewmodels.*

class MainActivity :
        AppCompatActivity(),
        EditCategoryInterface,
        ColorPickerDialogListener,
        ToastInterface,
        ZoomInterface,
        ActionMode.Callback {
    private val categoryListViewModel by viewModels<CategoryListViewModel>()
    private val appViewModel by viewModels<AppViewModel>()
    private val soundViewModel by viewModels<SoundViewModel>()
    private val soundAddViewModel by viewModels<SoundAddViewModel>()
    private val soundEditMultipleViewModel by viewModels<SoundEditMultipleViewModel>()

    private val actionbarLogoTouchTimes = mutableListOf<Long>()
    private var actionMode: ActionMode? = null
    private var categories = emptyList<Category>()
    private var filterEnabled: Boolean = false
    private var filterWasEnabled: Boolean = false
    private var reorderEnabled: Boolean? = null
    private var repressMode: SoundPlayer.RepressMode? = null
    private var sounds = emptyList<Sound>()  // used for soundAddViewModel
    private var toast: Toast? = null

    private lateinit var binding: ActivityMainBinding

    /** Overridden Android methods */
    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_all_sounds -> {
                soundViewModel.selectAll()
                true
            }
            R.id.edit_sounds -> {
                when {
                    soundViewModel.selectedSounds.size == 1 -> {
                        val sound = soundViewModel.selectedSounds.first()
                        showEditSoundDialogFragment(sound)
                    }
                    soundViewModel.selectedSounds.size > 1 -> {
                        soundEditMultipleViewModel.setup(
                                soundViewModel.selectedSounds.mapNotNull { it.id }, getString(R.string.multiple_sounds_selected))
                        showDialogFragment(EditMultipleSoundDialogFragment())
                    }
                }
                true
            }
            R.id.delete_sounds -> {
                when {
                    soundViewModel.selectedSounds.size == 1 -> {
                        val sound = soundViewModel.selectedSounds.first()
                        sound.id?.let { soundId ->
                            showDialogFragment(DeleteSoundFragment.newInstance(soundId, sound.name))
                        }
                    }
                    soundViewModel.selectedSounds.size > 1 ->
                        showDialogFragment(DeleteSoundFragment.newInstance(soundViewModel.selectedSounds.map { it.id }))
                }
                true
            }
            else -> false
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
                    soundAddViewModel.setup(sounds, this.sounds, getString(R.string.multiple_sounds_selected))
                    if (soundAddViewModel.isDuplicate) {
                        val fragment = AddDuplicateSoundDialogFragment.newInstance(true).apply {
                            setOnAddDuplicateListener {
                                soundAddViewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.ADD
                                showDialogFragment(AddMultipleSoundDialogFragment())
                            }
                            setOnUpdateExistingListener {
                                soundAddViewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.UPDATE
                                showDialogFragment(AddMultipleSoundDialogFragment())
                            }
                            setOnSkipDuplicatesListener {
                                soundAddViewModel.duplicateStrategy = SoundAddViewModel.DuplicateStrategy.SKIP
                                when (soundAddViewModel.sounds.size) {
                                    0 -> showToast(R.string.no_sounds_to_add)
                                    1 -> showDialogFragment(AddSoundDialogFragment())
                                    else -> showDialogFragment(AddMultipleSoundDialogFragment())
                                }
                            }
                        }
                        showDialogFragment(fragment)
                    } else showDialogFragment(AddMultipleSoundDialogFragment())
                }
            } else {
                // One item selected; data.data is a Uri
                data.data?.let { uri ->
                    val sound = Sound(uri, data.flags, contentResolver)
                    soundAddViewModel.setup(sound, sounds)
                    //soundAddViewModel.setup(sound, sounds)
                    if (soundAddViewModel.isDuplicate) {
                        val fragment = AddDuplicateSoundDialogFragment().apply {
                            setOnAddDuplicateListener { showDialogFragment(AddSoundDialogFragment()) }
                            setOnUpdateExistingListener {
                                it?.let { sound -> showEditSoundDialogFragment(sound) }
                            }
                        }
                        showDialogFragment(fragment)
                    } else showDialogFragment(AddSoundDialogFragment())
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(LOG_TAG, "width=${windowManager.currentWindowMetrics.bounds.width()}, height=${windowManager.currentWindowMetrics.bounds.height()}")
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

        soundViewModel.sounds.observe(this) {
            sounds = it
            //soundViewModel.initPlayers(it, this)
        }

        categoryListViewModel.categories.observe(this) {
            // Keep track of these to be able to send categoryIndex to EditSoundDialogFragment
            categories = it
            if (it.isEmpty()) categoryListViewModel.create(getString(R.string.default_category))
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.actionmode_menu, menu)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.appbar_menu, menu)
        // This has to be done here, because the callback requires the menu to exist
        appViewModel.repressMode.observe(this) { onRepressModeChange(it) }
        appViewModel.zoomInPossible.observe(this) { onZoomInPossibleChange(it) }
        soundViewModel.filterEnabled.observe(this) { onFilterEnabledChange(it) }
        soundViewModel.reorderEnabled.observe(this) { onReorderEnabledChange(it) }
        soundViewModel.undoAvailable.observe(this) { onUndosAvailableChange(it) }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) = soundViewModel.disableSelect()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_category -> showAddCategoryFragment()
            R.id.action_add_sound -> startAddSoundActivity()
            // R.id.action_reinit_failed_sounds -> reinitFailedSounds()
            R.id.action_set_repress_mode -> appViewModel.cycleRepressMode()
            R.id.action_toggle_filter -> soundViewModel.toggleFilterEnabled()
            R.id.action_toggle_reorder -> soundViewModel.toggleReorderEnabled()
            R.id.action_undo -> soundViewModel.undo()
            R.id.action_zoom_in -> zoomIn()
            R.id.action_zoom_out -> zoomOut()
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

    private fun onUndosAvailableChange(value: Boolean?) {
        // There are 1 or more undos in the queue
        val undoItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_undo)
                ?: binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_undo)
        when (value) {
            true -> undoItem?.icon?.alpha = 255
            else -> undoItem?.icon?.alpha = 128
        }
    }


    /**
     * Overridden 3rd party methods
     */
    override fun onColorSelected(dialogId: Int, color: Int) {
        // Have to do this just because ColorPickerDialog won't accept a Fragment as context :/
        (supportFragmentManager.findFragmentByTag(DIALOG_TAGS[dialogId]) as ColorPickerDialogListener).onColorSelected(dialogId, color)
    }

    override fun onDialogDismissed(dialogId: Int) = Unit


    /**
     * Overridden own methods
     */
    override fun showCategoryDeleteDialog(id: Int, name: String, soundCount: Int) =
            showDialogFragment(DeleteCategoryFragment.newInstance(id, name, soundCount, categories.size))

    override fun showCategoryEditDialog(categoryId: Int) =
            showDialogFragment(EditCategoryDialogFragment.newInstance(categoryId, DIALOG_TAGS.indexOf(CATEGORY_EDIT_DIALOG_TAG)), CATEGORY_EDIT_DIALOG_TAG)

    override fun showToast(text: CharSequence) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT).apply { show() }
    }

    override fun showToast(textResource: Int) = showToast(getText(textResource))

    override fun zoomIn() = appViewModel.zoomIn()?.let { showToast(getString(R.string.zoom_level_percent, it)) }

    override fun zoomOut() = appViewModel.zoomOut()?.let { showToast(getString(R.string.zoom_level_percent, it)) }


    /**
     * Own methods
     */
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

    private fun onReorderEnabledChange(value: Boolean) {
        val item = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_toggle_reorder)
        val toggleFilterItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_toggle_filter)
//        val undoItem = binding.actionbar.actionbarToolbar.menu?.findItem(R.id.action_undo)
//                ?: binding.bottombar.bottombarToolbar?.menu?.findItem(R.id.action_undo)
        if (value) {
            if (reorderEnabled != null) showToast(R.string.reordering_enabled)
            item?.icon?.alpha = 255
            filterWasEnabled = filterEnabled
            toggleFilterItem?.isVisible = false
            soundViewModel.disableFilter()
            //soundViewModel.enableUndo()
            //undoItem?.isVisible = true
            //undoItem?.icon?.alpha = 128
        } else {
            if (reorderEnabled != null) showToast(R.string.reordering_disabled)
            item?.icon?.alpha = 128
            toggleFilterItem?.isVisible = true
            if (filterWasEnabled) soundViewModel.enableFilter()
            //soundViewModel.disableUndo()
            //undoItem?.isVisible = false
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
        if (this.repressMode != null) showToast(getString(R.string.on_repress, mode))
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
            if (intent.resolveActivity(packageManager) != null) startActivityForResult(intent, REQUEST_SOUND_REINIT)
        }
    }

    private fun setupBottomBar() {
        //binding.bottombar.bottombarToolbar.inflateMenu(R.menu.bottom_menu)
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

    private fun showAddCategoryFragment() =
            showDialogFragment(AddCategoryDialogFragment.newInstance(DIALOG_TAGS.indexOf(CATEGORY_ADD_DIALOG_TAG)), CATEGORY_ADD_DIALOG_TAG)

    private fun showDialogFragment(fragment: Fragment, tag: String?) {
        supportFragmentManager.beginTransaction().apply {
            add(fragment, tag)
            show(fragment)
            commit()
        }
    }

    private fun showDialogFragment(fragment: Fragment) = showDialogFragment(fragment, null)

    private fun showEditSoundDialogFragment(sound: Sound) {
        var categoryIndex = categories.map { it.id }.indexOf(sound.categoryId)
        if (categoryIndex == -1) categoryIndex = 0
        sound.id?.let { soundId ->
            showDialogFragment(EditSoundDialogFragment.newInstance(soundId, categoryIndex))
        }
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


    companion object {
        const val REQUEST_SOUND_GET = 1
        const val REQUEST_SOUND_REINIT = 2
        const val EXTRA_SOUND_ID = "soundId"
        const val CATEGORY_ADD_DIALOG_TAG = "categoryAddDialog"
        const val CATEGORY_EDIT_DIALOG_TAG = "categoryEditDialog"
        const val LOG_TAG = "MainActity"
        val DIALOG_TAGS = listOf(CATEGORY_ADD_DIALOG_TAG, CATEGORY_EDIT_DIALOG_TAG)
    }
}