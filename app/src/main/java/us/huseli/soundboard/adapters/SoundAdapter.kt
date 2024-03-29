package us.huseli.soundboard.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.res.ColorStateList
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleRegistry
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
import us.huseli.soundboard.helpers.SoundPlayerTimer
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.CategoryViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt

class SoundAdapter(
    private val soundViewModel: SoundViewModel,
    private val appViewModel: AppViewModel,
    private val categoryViewModel: CategoryViewModel
) :
    LifecycleAdapter<SoundExtended, SoundAdapter.SoundViewHolder>(DiffCallback()) {

    var categoryId: Int? = null

    private var recyclerView: RecyclerView? = null

    init {
        setHasStableIds(true)
    }

    /*********** OVERRIDDEN/IMPLEMENTED METHODS ***********/
    override fun getItemId(position: Int): Long {
        try {
            return getItem(position).id!!.toLong()
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Sound at $position (${getItem(position)}) has null id")
            throw e
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position)
        if (BuildConfig.DEBUG)
            Log.d(
                LOG_TAG,
                "onBindViewHolder: item=$item, holder=$holder, position=$position, categoryId=$categoryId"
            )
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = SoundViewHolder(binding, appViewModel, soundViewModel)
        binding.lifecycleOwner = holder

        return holder
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundAdapter $hashCode <currentList=$currentList>"
    }


    /*********** OWN PUBLIC/INTERNAL METHODS ***********/
    internal fun collapseCategory() = categoryViewModel.collapse(categoryId)

    internal fun contains(sound: Sound) = currentList.indexOf(sound) > -1

    internal fun contains(soundId: Int) = currentList.map { it.id }.contains(soundId)

    internal fun expandCategory() = categoryViewModel.expand(categoryId)

    internal fun getAdapterPositionUnder(x: Float, y: Float): Int {
        recyclerView?.findChildViewUnder(x, y)?.let { view ->
            (recyclerView?.findContainingViewHolder(view) as? SoundViewHolder)?.let { viewHolder ->
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

    // internal fun getSoundAt(position: Int) = getItem(position)

    internal fun insertOrMoveSound(soundId: Int, toPosition: Int) {
        val fromPosition = currentList.map { it.id }.indexOf(soundId)
        val soundIds = currentList.mapNotNull { it.id }.toMutableList()

        if (BuildConfig.DEBUG)
            Log.i(
                LOG_TAG,
                "insertOrMoveSound: fromPosition=$fromPosition, toPosition=$toPosition, soundId=$soundId, this=$this, soundIds=$soundIds"
            )

        // "The construct when can have branches that overlap, in case of multiple matches the
        // first branch is chosen." -- https://superkotlin.com/kotlin-when-statement/
        when {
            toPosition == -1 -> soundIds.add(soundId)
            fromPosition == -1 -> soundIds.add(toPosition, soundId)
            fromPosition < toPosition -> for (i in fromPosition until toPosition - 1)
                Collections.swap(soundIds, i, i + 1)
            else -> for (i in fromPosition downTo toPosition + 1) Collections.swap(soundIds, i, i - 1)
        }
        soundViewModel.updateCategoryAndOrder(soundIds, categoryId)
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
        recyclerView?.also { rv ->
            if (adapterPosition > 0)
                (rv.findViewHolderForAdapterPosition(adapterPosition - 1) as SoundViewHolder)
                    .binding.dropMarkerAfter.visibility = View.VISIBLE
            if (adapterPosition < currentList.size)
                (rv.findViewHolderForAdapterPosition(adapterPosition) as SoundViewHolder)
                    .binding.dropMarkerBefore.visibility = View.VISIBLE
        }
    }

    internal fun removeMarksForDrop() {
        recyclerView?.also { rv ->
            for (i in 0..rv.childCount) {
                val child = rv.getChildAt(i)
                if (child != null) {
                    (rv.getChildViewHolder(child) as? SoundViewHolder)?.let {
                        it.binding.dropMarkerAfter.visibility = View.INVISIBLE
                        it.binding.dropMarkerBefore.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }


    companion object {
        const val LOG_TAG = "SoundAdapter"
    }


    class DiffCallback : DiffUtil.ItemCallback<SoundExtended>() {
        override fun areItemsTheSame(oldItem: SoundExtended, newItem: SoundExtended) = oldItem == newItem

        override fun areContentsTheSame(oldItem: SoundExtended, newItem: SoundExtended) =
            oldItem.name == newItem.name &&
                    // oldItem.order == newItem.order &&
                    oldItem.volume == newItem.volume &&
                    oldItem.backgroundColor == newItem.backgroundColor &&
                    oldItem.duration == newItem.duration
    }


    @SuppressLint("ClickableViewAccessibility")
    class SoundViewHolder(
        internal val binding: ItemSoundBinding,
        private val appViewModel: AppViewModel,
        private val soundViewModel: SoundViewModel
    ) :
        View.OnClickListener,
        View.OnLongClickListener,
        View.OnTouchListener,
        SoundPlayer.StateListener,
        SoundViewModel.SoundSelectionListener,
        LifecycleViewHolder<SoundExtended>(binding.root) {

        @InstallIn(SingletonComponent::class)
        @EntryPoint
        interface SoundViewHolderEntryPoint {
            fun playerRepository(): PlayerRepository
        }

        // private val appViewModel = adapter.appViewModel
        private val clickAnimator = (AnimatorInflater.loadAnimator(
            binding.root.context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }
        private val decimalFormat = DecimalFormat(".#").also {
            val symbols = it.decimalFormatSymbols
            symbols.decimalSeparator = '.'
            it.decimalFormatSymbols = symbols
        }

        // private val soundViewModel = adapter.soundViewModel
        private val playerRepository = EntryPointAccessors.fromApplication(
            binding.root.context.applicationContext,
            SoundViewHolderEntryPoint::class.java
        ).playerRepository()

        private var longClickAnimator: SoundItemLongClickAnimator? = null
        private var player: SoundPlayer? = null
        private var playerTimer: SoundPlayerTimer? = null

        override val lifecycleRegistry = LifecycleRegistry(this)
        override var item: SoundExtended? = null
        override val sound: Sound?
            get() = item

        init {
            binding.soundContainer.setOnClickListener(this)
            binding.soundContainer.setOnLongClickListener(this)
            binding.soundContainer.setOnTouchListener(this)
        }


        /********* PUBLIC/INTERNAL METHODS **********/
        internal fun bind(sound: SoundExtended) {
            item = sound
            val soundId = sound.id
            if (soundId == null) {
                Log.e(LOG_TAG, "bind: got Sound with id==null")
                return
            }

            binding.sound = sound

            setDuration(sound.duration)

            sound.backgroundColor?.also {
                binding.volumeBar.progressBackgroundTintList = ColorStateList.valueOf(it)
                longClickAnimator = SoundItemLongClickAnimator(binding.soundCard, it)
            }

            soundViewModel.addSoundSelectionListener(this)
            if (soundViewModel.isSelectEnabled && soundViewModel.isSoundSelected(sound)) onSelect()

            appViewModel.reorderEnabled.observe(this) { value -> onReorderEnabledChange(value) }

            playerRepository.players.observe(this) { players ->
                players[sound.id]?.also { newPlayer ->
                    player = newPlayer
                    newPlayer.setStateListener(this)
                    onSoundPlayerStateChange(newPlayer.state, newPlayer.playbackPositionMs)
                    appViewModel.repressMode.observe(this) { newPlayer.repressMode = it }
                }
            }
        }


        /********* PRIVATE METHODS **********/
        private fun onReorderEnabledChange(value: Boolean) {
            binding.reorderIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        private fun setDuration(value: Long) {
            /** We get value as milliseconds but display it as seconds */
            if (value > -1) {
                val durationString = when {
                    value < 950 -> decimalFormat.format(value.toDouble() / 1000)
                    else -> (value.toDouble() / 1000).roundToInt().toString()
                }
                binding.duration.text = binding.root.context.getString(R.string.duration_seconds, durationString)
                binding.durationCard.visibility = View.VISIBLE
                playerTimer?.also { timer ->
                    timer.setDuration(value)
                    item?.volume?.let { timer.setOriginalProgress(it) }
                } ?: run {
                    playerTimer = SoundPlayerTimer(value, binding.volumeBar, item?.volume ?: Constants.DEFAULT_VOLUME)
                }
            }
        }

        private fun showError() =
            player?.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }

        private fun startDragAndDrop(view: View) {
            item?.id?.let { soundId ->
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(view)
                val draggedSound = DraggedSound(soundId, bindingAdapterPosition, view.height)

                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "startDragAndDrop: draggedSound=$draggedSound, this=$this")

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    view.startDragAndDrop(data, shadowBuilder, draggedSound, 0)
                else
                    view.startDrag(data, shadowBuilder, draggedSound, 0)
            }
        }


        /********* OVERRIDDEN METHODS **********/
        override fun markDestroyed() {
            soundViewModel.removeOnSelectAllListener(this)
            playerRepository.players.removeObservers(this)
            appViewModel.reorderEnabled.removeObservers(this)
            appViewModel.repressMode.removeObservers(this)
            player?.setStateListener(null)
            super.markDestroyed()
        }

        override fun markDetach() {
            soundViewModel.removeOnSelectAllListener(this)
            playerRepository.players.removeObservers(this)
            appViewModel.repressMode.removeObservers(this)
            appViewModel.reorderEnabled.removeObservers(this)
            super.markDetach()
        }

        override fun onClick(view: View) {
            when {
                soundViewModel.isSelectEnabled -> soundViewModel.toggleSelect(item)
                player == null -> Snackbar.make(
                    binding.root,
                    R.string.soundplayer_not_initialized,
                    Snackbar.LENGTH_SHORT
                ).show()
                player?.state == SoundPlayer.State.ERROR -> showError()
                else -> player?.togglePlay(System.nanoTime() + Constants.SOUND_PLAY_TIMEOUT)
            }
            clickAnimator.start()
        }

        override fun onDeselect() {
            binding.selectedIcon.visibility = View.INVISIBLE
        }

        override fun onLongClick(v: View): Boolean {
            if (appViewModel.reorderEnabled.value != true) {
                longClickAnimator?.start()
                if (!soundViewModel.isSelectEnabled) {
                    // Select is not enabled; enable it and select sound
                    soundViewModel.enableSelect()
                    appViewModel.disableReorder()
                    soundViewModel.select(item)
                } else {
                    // Select is enabled; if this sound is not selected, select it and all
                    // between it and the last selected one (if any)
                    soundViewModel.selectAllFromSoundToLastSelected(item)
                }
            }
            return true
        }

        override fun onSelect() {
            binding.selectedIcon.visibility = View.VISIBLE
        }

        override fun onSoundPlayerStateChange(state: SoundPlayer.State, playbackPositionMs: Long) {
            /**
             * This will likely be called from a non-UI thread, hence View.post()
             * https://developer.android.com/guide/components/processes-and-threads#WorkerThreads
             */
            binding.root.post {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG,"onSoundPlayerStateChange: item=$item, state=$state")

                when (state) {
                    SoundPlayer.State.INITIALIZING -> Unit
                    SoundPlayer.State.STOPPED -> playerTimer?.stop()
                    SoundPlayer.State.PLAYING -> {
                        playerTimer?.start(playbackPositionMs)
                        binding.playIcon.visibility = View.VISIBLE
                    }
                    SoundPlayer.State.PAUSED -> {
                        playerTimer?.pause(playbackPositionMs)
                        binding.pauseIcon.visibility = View.VISIBLE
                    }
                    SoundPlayer.State.ERROR -> binding.failIcon.visibility = View.VISIBLE
                    SoundPlayer.State.RELEASED -> Unit
                    SoundPlayer.State.READY -> playerTimer?.stop()
                }

                if (state != SoundPlayer.State.PLAYING) binding.playIcon.visibility = View.INVISIBLE
                if (state != SoundPlayer.State.PAUSED) binding.pauseIcon.visibility = View.INVISIBLE

                /**
                 * Subdued colours are set by default on init; after that, only set them again on RELEASED or ERROR.
                 * The short flashing is annoying otherwise.
                 */
                if (state == SoundPlayer.State.RELEASED || state == SoundPlayer.State.ERROR) {
                    binding.soundName.alpha = 0.5f
                    binding.duration.alpha = 0.5f
                } else {
                    binding.soundName.alpha = 1.0f
                    binding.duration.alpha = 1.0f
                }
            }
        }

        override fun onSoundPlayerWarning(message: String) =
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (appViewModel.reorderEnabled.value == true) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onTouch: run startDragAndDrop on $view")
                    startDragAndDrop(view)
                }
                return false
            }
            return view.onTouchEvent(event)
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