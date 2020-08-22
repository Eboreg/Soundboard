package us.huseli.soundboard_kotlin.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.interfaces.ItemTouchHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel


class SoundAdapter(private val fragment: Fragment, private val appViewModel: AppViewModel, private val categoryViewModel: CategoryViewModel) :
        DataBoundListAdapter<SoundViewModel, SoundAdapter.ViewHolder, ItemSoundBinding>(Companion),
        ItemTouchHelperAdapter {

    companion object: DiffUtil.ItemCallback<SoundViewModel>() {
        override fun areItemsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem === newItem
        override fun areContentsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem.id == newItem.id
    }

    init {
        registerAdapterDataObserver(AdapterDataObserver())
    }

    override fun submitList(list: List<SoundViewModel>?) {
        Log.i(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()} submitList: $list")
        super.submitList(list)
    }

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: SoundViewModel) {
        Log.i(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.bind(item)
        holder.binding.categoryViewModel = categoryViewModel
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)


    inner class AdapterDataObserver : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            categoryViewModel.soundListViewModel.updateSoundOrder(fromPosition, toPosition)
        }
    }

    inner class ViewHolder(binding: ItemSoundBinding, private val context: Context) :
            DataBoundViewHolder<ItemSoundBinding>(binding),
            View.OnClickListener,
            View.OnLongClickListener,
            PopupMenu.OnMenuItemClickListener,
            AppViewModelListenerInterface {
        internal lateinit var viewModel: SoundViewModel
        private var categoryId: Int? = null

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        fun bind(viewModel: SoundViewModel) {
            Log.i(GlobalApplication.LOG_TAG, "SoundAdapter.ViewHolder ${hashCode()} bind SoundViewModel ${viewModel.hashCode()}")
            binding.viewModel = viewModel
            this.viewModel = viewModel
            viewModel.player.setOnCompletionListener { pause() }
            if (!viewModel.player.isValid) binding.failIcon.visibility = View.VISIBLE
            appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
            viewModel.categoryId.observe(this, { categoryId = it })
        }

        override fun onReorderEnabledChange(value: Boolean) =
                if (value) binding.root.setOnLongClickListener(null) else binding.root.setOnLongClickListener(this)

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

        override fun onClick(v: View?) =
                if (!viewModel.player.isValid) showErrorToast() else if (viewModel.player.isPlaying) pause() else play()

        override fun toString() = super.toString() + " '" + binding.soundName.text + "'"

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.sound_context_menu_edit -> {
                    categoryId?.let { catId ->
                        viewModel.id?.let { soundId ->
                            (fragment.requireActivity() as EditSoundInterface).showSoundEditDialog(soundId, catId)
                        }
                    } ?: Toast.makeText(context, R.string.data_not_fetched_yet, Toast.LENGTH_SHORT).show()
                }
                R.id.sound_context_menu_delete -> viewModel.delete()
            }
            return true
        }

        private fun play() {
            Log.i(GlobalApplication.LOG_TAG, "SoundAdapter.ViewHolder ${hashCode()} play")
            viewModel.player.play()
            binding.playIcon.visibility = View.VISIBLE
        }

        private fun pause() {
            viewModel.player.pause()
            binding.playIcon.visibility = View.INVISIBLE
        }

        private fun showErrorToast() = Toast.makeText(context, viewModel.player.errorMessage, Toast.LENGTH_SHORT).show()
    }
}