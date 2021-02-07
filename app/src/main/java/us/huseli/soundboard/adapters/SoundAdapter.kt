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
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.DraggedSound
import us.huseli.soundboard.data.PlayerRepository
import us.huseli.soundboard.data.Sound
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
    LifecycleAdapter<Sound, SoundAdapter.SoundViewHolder>(DiffCallback()) {
    @Suppress("PrivatePropertyName")
    private val LOG_TAG = "SoundAdapter"

    var category: Category? = null
    private var selectEnabled = false

    init {
        setHasStableIds(true)
    }

    /*********** OVERRIDDEN/IMPLEMENTED METHODS ***********/
    override fun getItemId(position: Int): Long {
        try {
            return currentList[position].id!!.toLong()
        } catch (e: NullPointerException) {
            if (BuildConfig.DEBUG) Log.e(
                LOG_TAG,
                "Sound at $position (${currentList[position]}) has null id"
            )
            throw e
        }
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val item = getItem(position)
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG,
            "onBindViewHolder: item=$item, holder=$holder, position=$position"
        )
        category?.let { holder.bind(item, it) }
            ?: run { Log.e(LOG_TAG, "onBindViewHolder: category is null") }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = SoundViewHolder(binding, parent.context, this)
        // Log.d(LOG_TAG, "onCreateViewHolder: holder=$holder, adapter=$this")
        binding.lifecycleOwner = holder

        return holder
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundAdapter $hashCode <currentList=$currentList>"
    }


    /*********** OWN PUBLIC/INTERNAL METHODS ***********/
    internal fun collapseCategory() = categoryViewModel.collapse(category)

    internal fun containsSound(sound: Sound) = currentList.indexOf(sound) > -1

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

    internal fun insertOrMoveSound(sound: Sound, toPosition: Int) {
        val fromPosition = currentList.indexOf(sound)
        val sounds = currentList.toMutableList()

        if (BuildConfig.DEBUG) Log.i(
            LOG_TAG,
            "insertOrMoveSound: fromPosition=$fromPosition, toPosition=$toPosition, sound=$sound, this=$this, sounds=$sounds"
        )

        // "The construct when can have branches that overlap, in case of multiple matches the
        // first branch is chosen." -- https://superkotlin.com/kotlin-when-statement/
        when {
            toPosition == -1 -> sounds.add(sound)
            fromPosition == -1 -> sounds.add(toPosition, sound)
            fromPosition < toPosition -> for (i in fromPosition until toPosition - 1) Collections.swap(
                sounds,
                i,
                i + 1
            )
            else -> for (i in fromPosition downTo toPosition + 1) Collections.swap(sounds, i, i - 1)
        }

        appViewModel.pushSoundUndoState(activity)
        soundViewModel.update(sounds, category)
    }

    internal fun isEmpty() = currentList.isEmpty()

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
            val pos1 = currentList.indexOf(sound)
            val pos2 = currentList.indexOf(lastSelected)
            if (pos1 != -1 && pos2 != -1) {
                val start = if (pos1 < pos2) pos1 else pos2
                val end = if (start == pos1) pos2 else pos1
                for (pos in (start + 1) until end)
                    (recyclerView.findViewHolderForLayoutPosition(pos) as? SoundViewHolder)?.select()
            }
        }
    }


    class DiffCallback : DiffUtil.ItemCallback<Sound>() {
        override fun areItemsTheSame(oldItem: Sound, newItem: Sound) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Sound, newItem: Sound) =
            oldItem.name == newItem.name && oldItem.order == newItem.order && oldItem.volume == newItem.volume
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
        LifecycleViewHolder(binding.root) {

        @InstallIn(SingletonComponent::class)
        @EntryPoint
        interface SoundViewHolderEntryPoint {
            fun playerRepository(): PlayerRepository
            fun colorHelper(): ColorHelper
        }

        @Suppress("PrivatePropertyName")
        private val LOG_TAG = "SoundViewHolder"

        private val appViewModel = adapter.appViewModel
        private val clickAnimator = (AnimatorInflater.loadAnimator(
            context,
            R.animator.sound_item_click_animator
        ) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }
        private val soundViewModel = adapter.soundViewModel
        private val activity = adapter.activity
        private val colorHelper = EntryPointAccessors.fromApplication(
            activity.applicationContext, SoundViewHolderEntryPoint::class.java
        ).colorHelper()
        private val playerRepository = EntryPointAccessors.fromApplication(
            activity.applicationContext, SoundViewHolderEntryPoint::class.java
        ).playerRepository()

        private var longClickAnimator: SoundItemLongClickAnimator? = null
        private var player: SoundPlayer? = null
        private var playerLiveData: LiveData<SoundPlayer?>? = null
        private var playerTimer: SoundPlayerTimer? = null
        private var reorderEnabled = false
        private var sound: Sound? = null

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundContainer.setOnClickListener(this)
            binding.soundContainer.setOnLongClickListener(this)
            binding.soundContainer.setOnTouchListener(this)
            soundViewModel.addOnSelectAllListener(this)
        }


        /********* PUBLIC/INTERNAL METHODS **********/
        internal fun bind(sound: Sound, category: Category) {
            this.sound = sound
            val soundId = sound.id
            if (soundId == null) {
                if (BuildConfig.DEBUG) Log.e(LOG_TAG, "bind: got Sound with id==null")
                return
            }

            binding.sound = sound
            binding.category = category
            setBackgroundColor(category.backgroundColor)

            // Stop any old player observer
            playerLiveData?.removeObservers(this)

            playerLiveData = playerRepository.get(sound).also { liveData ->
                liveData.observe(this) { newPlayer ->
                    if (newPlayer != null && newPlayer != player) {
                        newPlayer.setListener(this)
                        onSoundPlayerStateChange(newPlayer, newPlayer.state)
                        setDuration(newPlayer.duration)
                        appViewModel.repressMode.observe(this) { newPlayer.repressMode = it }
                        player = newPlayer
                    }
                    // this.sound?.volume?.let { player?.volume = it }
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
            if (value && soundViewModel.selectedSounds.contains(sound)) binding.selectedIcon.visibility =
                View.VISIBLE
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
            sound?.let { sound ->
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
                    playerTimer = SoundPlayerTimer(value, binding.volumeBar, sound?.volume ?: 100)
                else
                    sound?.volume?.let { playerTimer?.originalProgress = it }
            }
        }

        private fun showError() =
            player?.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }

        private fun startDragAndDrop(view: View) {
            sound?.let { sound ->
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(view)
                val draggedSound = DraggedSound(sound, bindingAdapterPosition, view.height)

                if (BuildConfig.DEBUG) Log.d(
                    LOG_TAG,
                    "startDragAndDrop: draggedSound=$draggedSound, this=$this"
                )

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    view.startDragAndDrop(data, shadowBuilder, draggedSound, 0)
                else
                    view.startDrag(data, shadowBuilder, draggedSound, 0)
            }
        }


        /********* OVERRIDDEN METHODS **********/
        private fun deselect() {
            sound?.let { sound ->
                soundViewModel.deselect(sound)
                binding.selectedIcon.visibility = View.INVISIBLE
            }
        }

        override fun markDestroyed() {
            release()
            super.markDestroyed()
        }

        override fun onClick(view: View) {
            sound?.let { sound ->
                when {
                    adapter.selectEnabled -> if (!soundViewModel.isSelected(sound)) select() else deselect()
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
                    sound?.let { sound ->
                        if (!soundViewModel.isSelected(sound)) {
                            adapter.selectAllInBetween(sound)
                        }
                    }
                }
                select()
            }
            return true
        }

        override fun onSoundPlayerStateChange(player: SoundPlayer, state: SoundPlayer.State) {
            /**
             * This will likely be called from a non-UI thread, hence View.post()
             * https://developer.android.com/guide/components/processes-and-threads#WorkerThreads
             */
            binding.root.post {
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

                // At the moment a sound is only INITIALIZING just when it's created, but maybe in
                // the future there will be some re-initialization
                if (state == SoundPlayer.State.INITIALIZING) {
                    binding.soundLoading.visibility = View.VISIBLE
                    binding.soundName.visibility = View.INVISIBLE
                } else {
                    binding.soundLoading.visibility = View.INVISIBLE
                    binding.soundName.visibility = View.VISIBLE
                }
                if (state == SoundPlayer.State.READY) setDuration(player.duration)
                if (state == SoundPlayer.State.ERROR || state == SoundPlayer.State.INITIALIZING) binding.durationCard.visibility =
                    View.GONE
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
            sound?.let { sound ->
                soundViewModel.select(sound)
                binding.selectedIcon.visibility = View.VISIBLE
            }
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "SoundAdapter.ViewHolder $hashCode <adapterPosition=$bindingAdapterPosition, sound=$sound>"
        }
    }
}