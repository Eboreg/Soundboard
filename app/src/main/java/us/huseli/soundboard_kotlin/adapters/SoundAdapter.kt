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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel


class SoundAdapter(private val fragment: Fragment, private val appViewModel: AppViewModel, private val categoryViewModel: CategoryViewModel) :
        DataBoundListAdapter<SoundViewModel, SoundAdapter.ViewHolder, ItemSoundBinding>(Companion),
        ItemDragHelperAdapter {

    companion object : DiffUtil.ItemCallback<SoundViewModel>() {
        override fun areItemsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem === newItem
        override fun areContentsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem.id == newItem.id
    }

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: SoundViewModel) {
        Log.d(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.bind(item)
        holder.binding.categoryViewModel = categoryViewModel
    }

    // Called several times DURING move
    override fun onItemMove(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    override fun onItemMoved(fromPosition: Int, toPosition: Int) {
        categoryViewModel.soundListViewModel.updateSoundOrder(fromPosition, toPosition)
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
            binding.soundCard.setOnClickListener(this)
            binding.soundCard.setOnLongClickListener(this)
        }

        fun bind(viewModel: SoundViewModel) {
            Log.d(GlobalApplication.LOG_TAG, "SoundAdapter.ViewHolder ${hashCode()} bind SoundViewModel ${viewModel.hashCode()}")
            binding.viewModel = viewModel
            this.viewModel = viewModel
            if (!viewModel.player.isValid) binding.failIcon.visibility = View.VISIBLE
            appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
            viewModel.categoryId.observe(this, { categoryId = it })
            viewModel.isPlaying.observe(this, { onIsPlayingChange(it) })
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
            if (!viewModel.player.isValid) showErrorToast() else viewModel.player.playOrPause()

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
                        (fragment.requireActivity() as EditSoundInterface).showSoundEditDialog(viewModel.id!!, categoryId!!)
                    R.id.sound_context_menu_delete ->
                        (fragment.requireActivity() as EditSoundInterface).showSoundDeleteDialog(viewModel.id!!, viewModel.name.value!!)
                }
            } catch (e: NullPointerException) {
                Toast.makeText(context, R.string.data_not_fetched_yet, Toast.LENGTH_SHORT).show()
            }
            return true
        }

        private fun showErrorToast() = Toast.makeText(context, viewModel.player.errorMessage, Toast.LENGTH_SHORT).show()
    }
}