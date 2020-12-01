package us.huseli.soundboard_kotlin.adapters

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
import androidx.core.view.children
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.SoundPlayer
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.SoundItemLongClickAnimator
import us.huseli.soundboard_kotlin.data.AbstractSound
import us.huseli.soundboard_kotlin.data.DraggedSound
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ItemEmptySoundBinding
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.helpers.SoundPlayerTimer
import us.huseli.soundboard_kotlin.viewmodels.*
import java.util.*
import kotlin.math.roundToInt


class SoundAdapter(
        private val categoryViewModel: CategoryViewModel,
        private val recyclerView: RecyclerView,
        private val soundViewModel: SoundViewModel,
        private val appViewModel: AppViewModel) :
        DataBoundAdapter<Sound, SoundAdapter.SoundViewHolder, ItemSoundBinding>(DiffCallback()) {
    private val LOG_TAG = "SoundAdapter"

    init {
        setHasStableIds(true)
        registerAdapterDataObserver(DataObserver())
    }

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
    override fun bind(holder: SoundViewHolder, item: Sound, position: Int) = holder.bind(item, categoryViewModel)

    override fun createBinding(parent: ViewGroup, viewType: Int): ItemSoundBinding {
        return ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    }

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup): SoundViewHolder {
        return SoundViewHolder(binding, parent.context)
    }

    override fun getItemById(id: Int) = currentList.find { it.id == id }

    override fun getItemId(position: Int): Long {
        try {
            return currentList[position].id!!.toLong()
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Sound at $position (${currentList[position]}) has null id")
            throw e
        }
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "SoundAdapter $hashCode <currentList=$currentList>"
    }

    fun collapseCategory() = categoryViewModel.collapse()

    fun containsSound(sound: Sound) = currentList.indexOf(sound) > -1

    fun expandCategory() = categoryViewModel.expand()

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

        soundViewModel.update(sounds, categoryViewModel.categoryId)
        //categoryViewModel.updateSounds(sounds.filterIsInstance<Sound>())
    }

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

    fun isEmpty() = currentList.isEmpty()

    fun removeMarksForDrop() {
        recyclerView.children
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
        recyclerView.getChildAt(position)?.let { view ->
            view.visibility = View.VISIBLE
        }
    }

    fun getAdapterPositionUnder(x: Float, y: Float): Int {
        recyclerView.findChildViewUnder(x, y)?.let { view ->
            (recyclerView.findContainingViewHolder(view) as? SoundViewHolder)?.let { viewHolder ->
                /**
                 * view går från view.x till view.x + view.width
                 * Om vi är över första halvan av view, returnera position för view
                 * Om vi är över andra halvan, returnera position för view:n efter om någon
                 */
                val middleX = view.x + (view.width / 2)
                return if (x <= middleX) viewHolder.adapterPosition else viewHolder.adapterPosition + 1
            }
        }
        return currentList.size
    }


    companion object {
        const val EMPTY_SOUND_VIEW_TYPE = 2
    }


    class DiffCallback : DiffUtil.ItemCallback<Sound>() {
        override fun areItemsTheSame(oldItem: Sound, newItem: Sound): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Sound, newItem: Sound): Boolean {
            return oldItem.name == newItem.name && oldItem.order == newItem.order && oldItem.volume == newItem.volume
        }
    }


    abstract inner class AbstractViewHolder(binding: ViewDataBinding) : DataBoundViewHolder<ViewDataBinding, AbstractSound>(binding)


    inner class EmptySoundViewHolder(override val binding: ItemEmptySoundBinding) : AbstractViewHolder(binding) {
        override val lifecycleRegistry = LifecycleRegistry(this)
    }


    @SuppressLint("ClickableViewAccessibility")
    inner class SoundViewHolder(override val binding: ItemSoundBinding, private val context: Context) :
            DataBoundViewHolder<ItemSoundBinding, Sound>(binding),
            View.OnClickListener,
            View.OnLongClickListener,
            View.OnTouchListener,
            SoundPlayer.OnStateChangeListener,
            SoundViewModel.OnSelectAllListener {
        private val LOG_TAG = "SoundViewHolder"

        private val clickAnimator = (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
            setTarget(binding.soundCard)
        }

        private var sound: Sound? = null
        private var player: SoundPlayer? = null
        private var selectEnabled = false
        private var reorderEnabled = false
        private var playerTimer: SoundPlayerTimer? = null
        private var longClickAnimator: SoundItemLongClickAnimator? = null
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

            player = soundViewModel.getPlayer(sound, context).also { player ->
                if (player.noPermission) soundViewModel.addFailedSound(sound)
                else {
                    player.setOnStateChangeListener(this)
                    setDuration(player.duration)
                    appViewModel.repressMode.observe(this) { player.repressMode = it }
                }
            }

            this@SoundAdapter.categoryViewModel.backgroundColor.observe(this) { color ->
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
            selectEnabled = value
            if (!value) {
                onIsSelectedChange(false)
            }
        }

        private fun onIsSelectedChange(value: Boolean) {
            Log.d(LOG_TAG, "onIsSelectedChange<sound=$sound, value=$value")
            if (value) {
                binding.selectedIcon.visibility = View.VISIBLE
            } else {
                binding.selectedIcon.visibility = View.INVISIBLE
            }
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
            if (!reorderEnabled && !selectEnabled) {
                sound?.let { sound ->
                    longClickAnimator?.start()
                    soundViewModel.enableSelect()
                    soundViewModel.select(sound)
                    onIsSelectedChange(true)
                }
            }
            return true
        }

        override fun onClick(view: View) {
            sound?.let { sound ->
                when {
                    selectEnabled -> onIsSelectedChange(soundViewModel.toggleSelected(sound))
                    player?.state == SoundPlayer.State.ERROR -> showErrorToast()
                    else -> player?.togglePlay()
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

        override fun selectAllSounds() {
            sound?.let { sound ->
                soundViewModel.select(sound)
                onIsSelectedChange(true)
            }
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "SoundAdapter.ViewHolder $hashCode <adapterPosition=$adapterPosition, sound=$sound>"
        }

        private fun showErrorToast() = Toast.makeText(context, player?.errorMessage, Toast.LENGTH_SHORT).show()

        override fun markDestroyed() {
            soundViewModel.removeOnSelectAllListener(this)
            super.markDestroyed()
        }

    }
}