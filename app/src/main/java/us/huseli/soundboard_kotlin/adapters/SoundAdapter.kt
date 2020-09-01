package us.huseli.soundboard_kotlin.adapters

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.SoundPlayer
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel


class SoundAdapter(private val activity: EditSoundInterface, private val appViewModel: AppViewModel, private val soundListViewModel: SoundListViewModel) :
        DataBoundListAdapter<Sound, SoundAdapter.ViewHolder, ItemSoundBinding>(Companion),
        ItemDragHelperAdapter<Sound> {

    companion object : DiffUtil.ItemCallback<Sound>() {
        override fun areItemsTheSame(oldItem: Sound, newItem: Sound) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Sound, newItem: Sound) =
                oldItem.uri == newItem.uri && oldItem.name == newItem.name && oldItem.order == newItem.order
    }

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: Sound) {
        Log.d(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.bind(item)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemsReordered(newList: MutableList<Sound>) {
        soundListViewModel.updateOrder(newList)
    }

    override fun getMutableList(): MutableList<Sound> = currentList.toMutableList()


    inner class ViewHolder(binding: ItemSoundBinding, private val context: Context) :
            DataBoundViewHolder<ItemSoundBinding>(binding),
            View.OnClickListener,
            View.OnLongClickListener,
            PopupMenu.OnMenuItemClickListener,
            AppViewModelListenerInterface {
        private var isValid = true
        private lateinit var viewModel: SoundViewModel
        private lateinit var player: SoundPlayer
        private lateinit var sound: Sound
        private lateinit var soundName: String

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundCard.setOnClickListener(this)
            binding.soundCard.setOnLongClickListener(this)
        }

        fun bind(sound: Sound) {
            Log.d(GlobalApplication.LOG_TAG, "SoundAdapter.ViewHolder ${hashCode()} bind Sound $sound")

            this.sound = sound

            player = GlobalApplication.application.getPlayer(sound)
            viewModel = SoundViewModel(sound.id!!)
            binding.viewModel = viewModel

            viewModel.name.observe(this, { soundName = it })

            player.isValid.observe(this, { isValid ->
                this.isValid = isValid
                binding.failIcon.visibility = if (!isValid) View.VISIBLE else View.INVISIBLE
            })

            player.isPlaying.observe(this, { onIsPlayingChange(it) })

            appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
        }

        private fun onIsPlayingChange(value: Boolean) {
            binding.playIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun onReorderEnabledChange(value: Boolean) =
                if (value) binding.soundCard.setOnLongClickListener(null) else binding.soundCard.setOnLongClickListener(this)

        override fun onZoomLevelChange(value: Int) = Unit

        override fun onLongClick(v: View?): Boolean {
            // v = item_sound.root (ConstraintLayout)
            v?.let {
                PopupMenu(v.context, v).apply {
                    menuInflater.inflate(R.menu.sound_context_menu, menu)
                    setOnMenuItemClickListener(this@ViewHolder)
                    show()
                }
            }
            return true
        }

        override fun onClick(view: View?) {
            if (!isValid) showErrorToast() else player.playOrPause()

            view?.let {
                (AnimatorInflater.loadAnimator(context, R.animator.sound_item_animator) as AnimatorSet).apply {
                    setTarget(view)
                    start()
                }
            }
        }

        override fun toString() = super.toString() + " '" + binding.soundName.text + "'"

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            try {
                when (item?.itemId) {
                    R.id.sound_context_menu_edit ->
                        activity.showSoundEditDialog(sound.id!!, sound.categoryId!!)
                    R.id.sound_context_menu_delete ->
                        activity.showSoundDeleteDialog(sound.id!!, viewModel.name.value!!)
                }
            } catch (e: NullPointerException) {
                Toast.makeText(context, R.string.data_not_fetched_yet, Toast.LENGTH_SHORT).show()
            }
            return true
        }

        private fun showErrorToast() = Toast.makeText(context, player.errorMessage, Toast.LENGTH_SHORT).show()
    }
}