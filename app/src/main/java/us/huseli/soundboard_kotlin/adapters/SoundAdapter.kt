package us.huseli.soundboard_kotlin.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.helpers.ItemTouchHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel


class SoundAdapter(private val categoryViewModel: CategoryViewModel) : DataBoundListAdapter<SoundViewModel, SoundAdapter.ViewHolder, ItemSoundBinding>(Companion), ItemTouchHelperAdapter {
    var currentSoundId: Int? = null

    companion object: DiffUtil.ItemCallback<SoundViewModel>() {
        override fun areItemsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem === newItem
        override fun areContentsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem.id == newItem.id
    }

    override fun submitList(list: List<SoundViewModel>?) {
        Log.i(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()} submitList: $list")
        super.submitList(list)
    }

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) =
            ViewHolder(binding, this, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) = ItemSoundBinding.inflate(LayoutInflater.from(parent.context))

    override fun bind(holder: ViewHolder, item: SoundViewModel) {
        Log.i(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.bind(item)
        holder.binding.categoryViewModel = categoryViewModel
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    override fun onItemDismiss(position: Int) = Unit


    inner class ViewHolder(binding: ItemSoundBinding, private val adapter: SoundAdapter, private val context: Context) : DataBoundViewHolder<ItemSoundBinding>(binding), View.OnClickListener, View.OnLongClickListener {
        internal lateinit var viewModel: SoundViewModel

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
        }

        fun bind(viewModel: SoundViewModel) {
            binding.viewModel = viewModel
            this.viewModel = viewModel
            viewModel.player.setOnCompletionListener { pause() }
            context
        }

        override fun onLongClick(v: View?): Boolean {
            adapter.currentSoundId = viewModel.id
            v?.showContextMenu()
            return true
        }

        override fun onClick(v: View?) {
            if (!viewModel.player.isValid) showErrorToast() else if (viewModel.player.isPlaying) pause() else play()
        }

        override fun toString(): String {
            return super.toString() + " '" + binding.soundName.text + "'"
        }

        private fun play() {
            viewModel.player.play()
            binding.playIcon.visibility = View.VISIBLE
        }

        private fun pause() {
            viewModel.player.pause()
            binding.playIcon.visibility = View.INVISIBLE
        }

        private fun showErrorToast() {
            Toast.makeText(context, viewModel.player.errorMessage, Toast.LENGTH_SHORT).show()
        }

    }

}