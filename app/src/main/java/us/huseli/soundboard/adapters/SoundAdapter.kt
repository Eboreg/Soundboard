package us.huseli.soundboard.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.R
import us.huseli.soundboard.adapters.common.LifecycleAdapter
import us.huseli.soundboard.adapters.common.LifecycleViewHolder
import us.huseli.soundboard.animators.SoundItemLongClickAnimator
import us.huseli.soundboard.audio.SoundPlayer
import us.huseli.soundboard.data.*
import us.huseli.soundboard.databinding.ItemSoundBinding
import us.huseli.soundboard.helpers.ColorHelper
import us.huseli.soundboard.helpers.SoundPlayerTimer
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.CategoryViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel
import java.util.*
import kotlin.math.roundToInt

class SoundAdapter(
    private val recyclerView: RecyclerView,
    private val soundViewModel: SoundViewModel,
    private val appViewModel: AppViewModel,
    private val categoryViewModel: CategoryViewModel,
    private val activity: FragmentActivity
) :
    LifecycleAdapter<SoundWithCategory, SoundAdapter.SoundViewHolder>(DiffCallback()) {
    var category: Category? = null
    private var selectEnabled = false

    init {
        setHasStableIds(true)
    }

    /*********** OVERRIDDEN/IMPLEMENTED METHODS ***********/
    override fun getItemId(position: Int): Long {
        try {
            return getItem(position).sound.id!!.toLong()
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Sound at $position (${getItem(position)}) has null id")
            throw e
        }
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onBindViewHolder: item=$item, holder=$holder, position=$position")
        category?.let { holder.bind(item) }
            ?: run { Log.e(LOG_TAG, "onBindViewHolder: category is null") }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = SoundViewHolder(binding, parent.context, this)
        binding.lifecycleOwner = holder

        return holder
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundAdapter $hashCode <currentList=$currentList>"
    }


    /*********** OWN PUBLIC/INTERNAL METHODS ***********/
    internal fun collapseCategory() = categoryViewModel.collapse(category)

    internal fun contains(soundWithCategory: SoundWithCategory) =
        currentList.indexOf(soundWithCategory) > -1

    internal fun expandCategory() = categoryViewModel.expand(category)

    internal fun getAdapterPositionUnder(x: Float, y: Float): Int {
        recyclerView.findChildViewUnder(x, y)?.let { view ->
            (recyclerView.findContainingViewHolder(view) as? SoundViewHolder)?.let { viewHolder ->
                /**
                 * view spans horizontally from view.x to (view.x + view.width)
                 * If we are over view's first half, return view position
                 * If we are over view's second half, return view position + 1
                 */
                val middleX = view.x + (view.width / 2)
                return if (x <= middleX) viewHolder.bindingAdapterPosition else viewHolder.bindingAdapterPosition + 1
            }
        }
        return currentList.size
    }

    internal fun getSoundAt(position: Int) = getItem(position)

    internal fun insertOrMoveSound(soundWithCategory: SoundWithCategory, toPosition: Int) {
        val fromPosition = currentList.indexOf(soundWithCategory)
        val soundsWithCategory = currentList.toMutableList()

        if (BuildConfig.DEBUG)
            Log.i(LOG_TAG,
                "insertOrMoveSound: fromPosition=$fromPosition, toPosition=$toPosition, sound=$soundWithCategory, this=$this, sounds=$soundsWithCategory")

        // "The construct when can have branches that overlap, in case of multiple matches the
        // first branch is chosen." -- https://superkotlin.com/kotlin-when-statement/
        when {
            toPosition == -1 -> soundsWithCategory.add(soundWithCategory)
            fromPosition == -1 -> soundsWithCategory.add(toPosition, soundWithCategory)
            fromPosition < toPosition -> for (i in fromPosition until toPosition - 1) Collections.swap(
                soundsWithCategory,
                i,
                i + 1
            )
            else -> for (i in fromPosition downTo toPosition + 1) Collections.swap(
                soundsWithCategory,
                i,
                i - 1
            )
        }

        appViewModel.pushSoundUndoState(activity)
        soundViewModel.update(soundsWithCategory)
    }

    internal fun isEmpty() = currentList.isEmpty()

    internal fun isNotEmpty() = !isEmpty()

    internal fun markSoundsForDrop(adapterPosition: Int) {
        /**
         * adapterPosition = position we would move to, were we to drop right now
         * So highlight area between (adapterPosition - 1) and adapterPosition
         * That means: Highlight after viewHolder[adapterPosition - 1] and before
         *      viewHolder[adapterPosition]
         * If adapterPosition == 0, only do the "before" bit
         * If adapterPosition >= list size, only do the "after" bit
         * That means:
         * If adapterPosition > 0, do the "after" bit
         * If adapterPosition < list size, do the "before" bit
         */
        if (adapterPosition > 0) (recyclerView.findViewHolderForAdapterPosition(adapterPosition - 1) as SoundViewHolder).binding.dropMarkerAfter.visibility =
            View.VISIBLE
        if (adapterPosition < currentList.size) (recyclerView.findViewHolderForAdapterPosition(
            adapterPosition
        ) as SoundViewHolder).binding.dropMarkerBefore.visibility = View.VISIBLE
    }

    internal fun onSelectEnabledChange(value: Boolean) {
        selectEnabled = value
    }

    internal fun removeMarksForDrop() {
        for (i in 0..recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            if (child != null) {
                (recyclerView.getChildViewHolder(child) as? SoundViewHolder)?.let {
                    it.binding.dropMarkerAfter.visibility = View.INVISIBLE
                    it.binding.dropMarkerBefore.visibility = View.INVISIBLE
                }
            }
        }
    }


    /*********** PRIVATE METHODS ***********/
    private fun selectAllInBetween(sound: Sound) {
        // Select all sound between `sound` and last selected one (if any).
        soundViewModel.getLastSelected(category, sound)?.let { lastSelected ->
            // TODO: Are these always consistent with adapter/layout positions?
            val pos1 = currentList.map { it.sound }.indexOf(sound)
            val pos2 = currentList.map { it.sound }.indexOf(lastSelected)
            if (pos1 != -1 && pos2 != -1) {
                val start = if (pos1 < pos2) pos1 else pos2
                val end = if (start == pos1) pos2 else pos1
                for (pos in (start + 1) until end)
                    (recyclerView.findViewHolderForLayoutPosition(pos) as? SoundViewHolder)?.select()
            }
        }
    }


    companion object {
        const val LOG_TAG = "SoundAdapter"
    }


    class DiffCallback : DiffUtil.ItemCallback<SoundWithCategory>() {
        override fun areItemsTheSame(oldItem: SoundWithCategory, newItem: SoundWithCategory) =
            oldItem.sound.id == newItem.sound.id

        override fun areContentsTheSame(oldItem: SoundWithCategory, newItem: SoundWithCategory) =
            oldItem.sound.name == newItem.sound.name && oldItem.sound.order == newItem.sound.order && oldItem.sound.volume == newItem.sound.volume && oldItem.sound.backgroundColor == newItem.sound.backgroundColor
    }


    @SuppressLint("ClickableViewAccessibility")
    class SoundViewHolder(
        internal val binding: ItemSoundBinding,
        private val context: Context,
        private val adapter: SoundAdapter
    ) :
        View.OnClickListener,
        View.OnLongClickListener,
        View.OnTouchListener,
        SoundPlayer.Listener,
        SoundViewModel.OnSelectAllListener,
        LifecycleViewHolder<SoundWithCategory>(binding.root) {

        @InstallIn(SingletonComponent::class)
        @EntryPoint
        interface SoundViewHolderEntryPoint {
            fun playerRepository(): PlayerRepository
            fun colorHelper(): ColorHelper
        }

        private val appViewModel = adapter.appViewModel
        private val clickAnimator = (AnimatorInflater.loadAnimator(
            context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }
        private val soundViewModel = adapter.soundViewModel
        private val activity = adapter.activity
        private val colorHelper = EntryPointAccessors.fromApplication(
            activity.applicationContext, SoundViewHolderEntryPoint::class.java).colorHelper()
        private val playerRepository = EntryPointAccessors.fromApplication(
            activity.applicationContext, SoundViewHolderEntryPoint::class.java).playerRepository()

        private var longClickAnimator: SoundItemLongClickAnimator? = null
        private var player: SoundPlayer? = null
        private var playerLiveData: LiveData<SoundPlayer?>? = null
        private var playerTimer: SoundPlayerTimer? = null
        private var reorderEnabled = false

        override val lifecycleRegistry = LifecycleRegistry(this)
        override var item: SoundWithCategory? = null

        init {
            binding.soundContainer.setOnClickListener(this)
            binding.soundContainer.setOnLongClickListener(this)
            binding.soundContainer.setOnTouchListener(this)
            soundViewModel.addOnSelectAllListener(this)
        }


        /********* PUBLIC/INTERNAL METHODS **********/
        internal fun bind(soundWithCategory: SoundWithCategory) {
            item = soundWithCategory
            val soundId = soundWithCategory.sound.id
            if (soundId == null) {
                Log.e(LOG_TAG, "bind: got Sound with id==null")
                return
            }

            binding.sound = soundWithCategory.sound
            // TODO: Is this necessary?
            setBackgroundColor(soundWithCategory.category.backgroundColor)

            playerRepository.players.observe(this) { players ->
                // if (BuildConfig.DEBUG) Log.d(LOG_TAG, "playerRepository.players: players=$players")
                players?.firstOrNull { it.sound == soundWithCategory.sound }?.also { newPlayer ->
                    if (newPlayer != player) {
                        if (BuildConfig.DEBUG)
                            Log.d(LOG_TAG,
                                "playerRepository.players: newPlayer=$newPlayer, player=$player, state=${newPlayer.state}")
                        newPlayer.setListener(this)
                        onSoundPlayerStateChange(newPlayer, newPlayer.state)
                        setDuration(newPlayer.duration)
                        appViewModel.repressMode.observe(this) { newPlayer.repressMode = it }
                        player = newPlayer
                    }
                }
            }

            soundViewModel.reorderEnabled.observe(this) { value -> onReorderEnabledChange(value) }
            soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }
        }


        /********* PRIVATE METHODS **********/
        private fun onReorderEnabledChange(value: Boolean) {
            reorderEnabled = value
            binding.reorderIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        private fun onSelectEnabledChange(value: Boolean) {
            if (value && soundViewModel.selectedSounds.contains(item?.sound))
                binding.selectedIcon.visibility = View.VISIBLE
            else if (!value) binding.selectedIcon.visibility = View.INVISIBLE
        }

        private fun release() {
            soundViewModel.removeOnSelectAllListener(this)
            playerLiveData?.removeObservers(this)
            player?.setListener(null)
        }

        private fun setBackgroundColor(color: Int) {
            longClickAnimator = SoundItemLongClickAnimator(binding.soundCard, color)
            binding.volumeBar.progressDrawable.alpha = 150
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.volumeBar.progressTintMode = PorterDuff.Mode.OVERLAY
                binding.volumeBar.progressTintList =
                    if (colorHelper.getLuminance(color) >= 0.6) ColorStateList.valueOf(Color.BLACK)
                    else ColorStateList.valueOf(Color.WHITE)
            }
        }

        private fun setDuration(value: Int) {
            /** We get value as milliseconds but display it as seconds */
            item?.sound?.let { sound ->
                if (sound.duration != value) {
                    sound.duration = value
                    soundViewModel.updateDuration(sound, value)
                }
            }
            if (value > -1) {
                binding.duration.text = context.getString(
                    R.string.duration_seconds,
                    (value.toDouble() / 1000).roundToInt()
                )
                binding.durationCard.visibility = View.VISIBLE
                if (playerTimer?.duration != value)
                    playerTimer = SoundPlayerTimer(
                        value, binding.volumeBar, item?.sound?.volume ?: Constants.DEFAULT_VOLUME)
                else
                    item?.sound?.volume?.let { playerTimer?.originalProgress = it }
            }
        }

        private fun showError() =
            player?.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }

        private fun startDragAndDrop(view: View) {
            item?.let { item ->
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(view)
                val draggedSound = DraggedSound(item, bindingAdapterPosition, view.height)

                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "startDragAndDrop: draggedSound=$draggedSound, this=$this")

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    view.startDragAndDrop(data, shadowBuilder, draggedSound, 0)
                else
                    view.startDrag(data, shadowBuilder, draggedSound, 0)
            }
        }


        /********* OVERRIDDEN METHODS **********/
        private fun deselect() {
            item?.let { item ->
                soundViewModel.deselect(item.sound)
                binding.selectedIcon.visibility = View.INVISIBLE
            }
        }

        override fun markDestroyed() {
            release()
            super.markDestroyed()
        }

        override fun onClick(view: View) {
            item?.let { item ->
                when {
                    adapter.selectEnabled -> if (!soundViewModel.isSelected(item.sound)) select() else deselect()
                    player == null -> Snackbar.make(
                        binding.root,
                        R.string.soundplayer_not_initialized,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    player?.state == SoundPlayer.State.ERROR -> showError()
                    else -> player?.togglePlay()
                }
                clickAnimator.start()
            }
        }

        override fun onLongClick(v: View): Boolean {
            if (!reorderEnabled) {
                longClickAnimator?.start()
                if (!adapter.selectEnabled) {
                    // Select is not enabled; enable it
                    soundViewModel.enableSelect()
                } else {
                    // Select is enabled; if this sound is not selected, select it and all
                    // between it and the last selected one (if any)
                    item?.let { item ->
                        if (!soundViewModel.isSelected(item.sound)) {
                            adapter.selectAllInBetween(item.sound)
                        }
                    }
                }
                select()
            }
            return true
        }

        override fun onSoundPlayerDurationChange(duration: Int) = activity.runOnUiThread { setDuration(duration) }

        override fun onSoundPlayerStateChange(player: SoundPlayer, state: SoundPlayer.State) {
            /**
             * This will likely be called from a non-UI thread, hence View.post()
             * https://developer.android.com/guide/components/processes-and-threads#WorkerThreads
             */
            binding.root.post {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onSoundPlayerStateChange: item=$item, state=$state")

                if (state == SoundPlayer.State.PLAYING) {
                    playerTimer?.start()
                    binding.playIcon.visibility = View.VISIBLE
                } else binding.playIcon.visibility = View.INVISIBLE

                if (state == SoundPlayer.State.STOPPED || state == SoundPlayer.State.READY) {
                    playerTimer?.apply {
                        cancel()
                        onFinish()
                    }
                }

                binding.failIcon.visibility =
                    if (state == SoundPlayer.State.ERROR) View.VISIBLE else View.INVISIBLE

                binding.soundLoading.visibility =
                    if (listOf(SoundPlayer.State.INITIALIZING,
                            SoundPlayer.State.STOPPED,
                            SoundPlayer.State.RELEASED).contains(state))
                        View.VISIBLE
                    else View.INVISIBLE
            }
        }

        override fun onSoundPlayerWarning(message: String) =
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (reorderEnabled) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onTouch: run startDragAndDrop on $view")
                    startDragAndDrop(view)
                }
                return false
            }
            return view.onTouchEvent(event)
        }

        override fun select() {
            item?.let { item ->
                soundViewModel.select(item.sound)
                binding.selectedIcon.visibility = View.VISIBLE
            }
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "SoundAdapter.ViewHolder $hashCode <adapterPosition=$bindingAdapterPosition, sound=$item>"
        }


        companion object {
            const val LOG_TAG = "SoundViewHolder"
        }
    }
}