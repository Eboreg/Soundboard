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
import android.widget.Toast
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.R
import us.huseli.soundboard.SoundPlayer
import us.huseli.soundboard.adapters.common.LifecycleAdapter
import us.huseli.soundboard.adapters.common.LifecycleViewHolder
import us.huseli.soundboard.animators.SoundItemLongClickAnimator
import us.huseli.soundboard.data.DraggedSound
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.ItemSoundBinding
import us.huseli.soundboard.helpers.SoundPlayerTimer
import us.huseli.soundboard.viewmodels.*
import java.util.*
import kotlin.math.roundToInt


class SoundAdapter(
        private val recyclerView: RecyclerView,
        private val soundViewModel: SoundViewModel,
        private val appViewModel: AppViewModel) :
        LifecycleAdapter<Sound, SoundAdapter.SoundViewHolder>(DiffCallback()) {
    @Suppress("PrivatePropertyName")
    private val LOG_TAG = "SoundAdapter"
    private val players = hashMapOf<Int, SoundPlayer>()

    private var selectEnabled = false

    var categoryViewModel: CategoryViewModel? = null

    init {
        setHasStableIds(true)
        //registerAdapterDataObserver(DataObserver())
    }

    // TODO: Remove
    inner class DataObserver : RecyclerView.AdapterDataObserver() {
        @Suppress("PropertyName")
        val LOG_TAG = "DataObserver"

        override fun onChanged() {
            Log.d(LOG_TAG, "onChanged ---- adapter=${this@SoundAdapter}")
            super.onChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeChanged<positionStart=$positionStart, itemCount=$itemCount> ---- adapter=${this@SoundAdapter}")
            super.onItemRangeChanged(positionStart, itemCount)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            Log.d(LOG_TAG, "onItemRangeChanged<positionStart=$positionStart, itemCount=$itemCount, payload=$payload> ---- adapter=${this@SoundAdapter}")
            super.onItemRangeChanged(positionStart, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeInserted<positionStart=$positionStart, itemCount=$itemCount> ---- adapter=${this@SoundAdapter}")
            super.onItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeRemoved<positionStart=$positionStart, itemCount=$itemCount> ---- adapter=${this@SoundAdapter}")
            super.onItemRangeRemoved(positionStart, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            Log.d(LOG_TAG, "onItemRangeMoved<fromPosition=$fromPosition, toPosition=$toPosition, itemCount=$itemCount> ---- adapter=${this@SoundAdapter}")
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
        }
    }

    /**
     * Various implemented/overridden methods
     */
    override fun onCurrentListChanged(previousList: List<Sound>, currentList: List<Sound>) {
        // Get players for newly added sounds
        currentList.subtract(previousList).forEach {
            it.id?.let { soundId -> players[soundId] = soundViewModel.getPlayer(it, recyclerView.context) }
        }
        // Remove references to players for sounds no longer present
        previousList.subtract(currentList).forEach { players.remove(it.id) }
    }

    override fun getItemId(position: Int): Long {
        try {
            return currentList[position].id!!.toLong()
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Sound at $position (${currentList[position]}) has null id")
            throw e
        }
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        val item = getItem(position)
        Log.i(LOG_TAG, "onBindViewHolder: item=$item, holder=$holder, position=$position, adapter=$this")
        categoryViewModel?.let { holder.bind(item, it) }
                ?: run { Log.e(LOG_TAG, "onBindViewHolder: categoryViewModel is null") }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val binding = ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = SoundViewHolder(binding, parent.context)
        Log.i(LOG_TAG, "onCreateViewHolder: holder=$holder, adapter=$this")
        binding.lifecycleOwner = holder

        return holder
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundAdapter $hashCode <currentList=$currentList>"
    }

    /**
     * Own public methods
     */
    fun collapseCategory() = categoryViewModel?.collapse()

    fun containsSound(sound: Sound) = currentList.indexOf(sound) > -1

    fun expandCategory() = categoryViewModel?.expand()

    fun getAdapterPositionUnder(x: Float, y: Float): Int {
        recyclerView.findChildViewUnder(x, y)?.let { view ->
            (recyclerView.findContainingViewHolder(view) as? SoundViewHolder)?.let { viewHolder ->
                /**
                 * view spans horizontally from view.x to (view.x + view.width)
                 * If we are over view's first half, return view position
                 * If we are over view's second half, return view position + 1
                 */
                val middleX = view.x + (view.width / 2)
                return if (x <= middleX) viewHolder.adapterPosition else viewHolder.adapterPosition + 1
            }
        }
        return currentList.size
    }

    fun insertOrMoveSound(sound: Sound, toPosition: Int) {
        val fromPosition = currentList.indexOf(currentList.filterIsInstance<Sound>().find { it.id == sound.id })
        val sounds = currentList.toMutableList()

        Log.i(LOG_TAG, "insertOrMoveSound: fromPosition=$fromPosition, toPosition=$toPosition, sound=$sound, this=$this, sounds=$sounds")

        // "The construct when can have branches that overlap, in case of multiple matches the
        // first branch is chosen." -- https://superkotlin.com/kotlin-when-statement/
        when {
            toPosition == -1 -> sounds.add(sound)
            fromPosition == -1 -> sounds.add(toPosition, sound)
            fromPosition < toPosition -> for (i in fromPosition until toPosition - 1) Collections.swap(sounds, i, i + 1)
            else -> for (i in fromPosition downTo toPosition + 1) Collections.swap(sounds, i, i - 1)
        }

        categoryViewModel?.let { soundViewModel.update(sounds, it.categoryId) }
    }

    fun isEmpty() = currentList.isEmpty()

    fun markSoundsForDrop(adapterPosition: Int) {
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
        if (adapterPosition > 0) (recyclerView.findViewHolderForAdapterPosition(adapterPosition - 1) as SoundViewHolder).binding.dropMarkerAfter.visibility = View.VISIBLE
        if (adapterPosition < currentList.size) (recyclerView.findViewHolderForAdapterPosition(adapterPosition) as SoundViewHolder).binding.dropMarkerBefore.visibility = View.VISIBLE
    }

    fun onSelectEnabledChange(value: Boolean) {
        selectEnabled = value
    }

    fun removeMarksForDrop() {
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

    fun showSound(sound: Sound) {
        val position = currentList.indexOf(sound)
        recyclerView.getChildAt(position)?.let { view -> view.visibility = View.VISIBLE }
    }

    /**
     * Private methods
     */
    private fun selectAllInBetween(sound: Sound) {
        // Select all sound between `sound` and last selected one (if any).
        categoryViewModel?.categoryId?.let { categoryId ->
            soundViewModel.getLastSelected(categoryId, sound)?.let { lastSelected ->
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
    }


    class DiffCallback : DiffUtil.ItemCallback<Sound>() {
        override fun areItemsTheSame(oldItem: Sound, newItem: Sound) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Sound, newItem: Sound) =
                oldItem.name == newItem.name && oldItem.order == newItem.order && oldItem.volume == newItem.volume
    }


    @SuppressLint("ClickableViewAccessibility")
    inner class SoundViewHolder(val binding: ItemSoundBinding, private val context: Context) :
            View.OnClickListener,
            View.OnLongClickListener,
            View.OnTouchListener,
            SoundPlayer.OnStateChangeListener,
            SoundViewModel.OnSelectAllListener,
            LifecycleViewHolder(binding.root) {
        @Suppress("PrivatePropertyName")
        private val LOG_TAG = "SoundViewHolder"

        private val clickAnimator = (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }

        private var longClickAnimator: SoundItemLongClickAnimator? = null
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

        fun bind(sound: Sound, categoryViewModel: CategoryViewModel) {
            Log.i(LOG_TAG, "bind: sound=$sound ----- adapter=${this@SoundAdapter} ----- viewHolder=$this ----- categoryViewModel=${this@SoundAdapter.categoryViewModel}")

            this.sound = sound
            binding.sound = sound

            binding.categoryViewModel = categoryViewModel

            players[sound.id]?.also { player ->
                if (player.noPermission) soundViewModel.addFailedSound(sound)
                else {
                    player.setOnStateChangeListener(this)
                    setDuration(player.duration)
                    appViewModel.repressMode.observe(this) { player.repressMode = it }
                }
            }

            this@SoundAdapter.categoryViewModel?.backgroundColor?.observe(this) { color ->
                longClickAnimator = SoundItemLongClickAnimator(binding.soundCard, color)
                binding.volumeBar.progressDrawable.alpha = 150
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    binding.volumeBar.progressTintMode = PorterDuff.Mode.OVERLAY
                    binding.volumeBar.progressTintList =
                            if (GlobalApplication.application.getColorHelper().getLuminance(color) >= 0.6) ColorStateList.valueOf(Color.BLACK)
                            else ColorStateList.valueOf(Color.WHITE)
                }
            }

            soundViewModel.reorderEnabled.observe(this) { value -> onReorderEnabledChange(value) }
            soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }
        }

        private fun setDuration(value: Int) {
            /**
             * We get value as milliseconds but display it as seconds
             */
            if (value > -1) {
                binding.duration.text = context.getString(R.string.duration_seconds, (value.toDouble() / 1000).roundToInt())
                binding.durationCard.visibility = View.VISIBLE
                if (playerTimer?.duration != value)
                    playerTimer = SoundPlayerTimer(value, binding.volumeBar, sound?.volume ?: 100)
            }
        }

        private fun onSelectEnabledChange(value: Boolean) {
            if (value && soundViewModel.selectedSounds.contains(sound)) binding.selectedIcon.visibility = View.VISIBLE
            else if (!value) binding.selectedIcon.visibility = View.INVISIBLE
        }

        private fun onReorderEnabledChange(value: Boolean) {
            reorderEnabled = value
            binding.reorderIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (reorderEnabled) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    Log.d(LOG_TAG, "onTouch: run startDragAndDrop on $view")
                    startDragAndDrop(view)
                }
                return false
            }
            return view.onTouchEvent(event)
        }

        override fun onLongClick(v: View): Boolean {
            if (!reorderEnabled) {
                sound?.let { sound ->
                    longClickAnimator?.start()
                    if (!selectEnabled) {
                        // Select is not enabled; enable it
                        soundViewModel.enableSelect()
                    } else {
                        // Select is enabled; if this sound is not selected, select it and all
                        // between it and the last selected one (if any)
                        if (!soundViewModel.isSelected(sound)) {
                            selectAllInBetween(sound)
                        }
                    }
                    select()
                }
            }
            return true
        }

        override fun onClick(view: View) {
            sound?.let { sound ->
                when {
                    selectEnabled -> if (!soundViewModel.isSelected(sound)) select() else deselect()
                    players[sound.id]?.state == SoundPlayer.State.ERROR -> showErrorToast()
                    else -> players[sound.id]?.togglePlay()
                }
                clickAnimator.start()
            }
        }

        private fun startDragAndDrop(view: View) {
            sound?.let { sound ->
                val data = ClipData.newPlainText("", "")
                val shadowBuilder = View.DragShadowBuilder(view)
                val draggedSound = DraggedSound(sound, adapterPosition)

                Log.d(LOG_TAG, "startDragAndDrop: draggedSound=$draggedSound, this=$this")

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val retval = view.startDragAndDrop(data, shadowBuilder, draggedSound, 0)
                    @Suppress("ControlFlowWithEmptyBody")
                    if (!retval)
                        Log.e(LOG_TAG, "startDragAndDrop: view.startDragAndDrop() returned false")
                    else {
                    }
                } else
                    view.startDrag(data, shadowBuilder, draggedSound, 0)
            }
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
                if (state == SoundPlayer.State.STOPPED) {
                    playerTimer?.apply {
                        cancel()
                        onFinish()
                    }
                }
                binding.failIcon.visibility = if (state == SoundPlayer.State.ERROR) View.VISIBLE else View.INVISIBLE
                if (state == SoundPlayer.State.INITIALIZING) {
                    binding.soundLoading.visibility = View.VISIBLE
                    binding.soundName.visibility = View.INVISIBLE
                } else {
                    binding.soundLoading.visibility = View.INVISIBLE
                    binding.soundName.visibility = View.VISIBLE
                }
                if (state == SoundPlayer.State.READY) setDuration(player.duration)
                if (state == SoundPlayer.State.ERROR || state == SoundPlayer.State.INITIALIZING) binding.durationCard.visibility = View.GONE
            }
        }

        override fun select() {
            sound?.let { sound ->
                soundViewModel.select(sound)
                binding.selectedIcon.visibility = View.VISIBLE
            }
        }

        private fun deselect() {
            sound?.let { sound ->
                soundViewModel.deselect(sound)
                binding.selectedIcon.visibility = View.INVISIBLE
            }
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "SoundAdapter.ViewHolder $hashCode <adapterPosition=$adapterPosition, sound=$sound>"
        }

        private fun showErrorToast() = Toast.makeText(context, players[sound?.id]?.errorMessage, Toast.LENGTH_SHORT).show()

        override fun markDestroyed() {
            soundViewModel.removeOnSelectAllListener(this)
            super.markDestroyed()
        }

    }

}