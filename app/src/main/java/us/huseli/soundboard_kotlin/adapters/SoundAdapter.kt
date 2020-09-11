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
import androidx.cardview.widget.CardView
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.SoundItemLongClickAnimator
import us.huseli.soundboard_kotlin.databinding.ItemSoundBinding
import us.huseli.soundboard_kotlin.fragments.CategoryListFragment
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditSoundInterface
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.SoundViewModel


class SoundAdapter(val fragment: CategoryListFragment) :
        DataBoundListAdapter<SoundViewModel, SoundAdapter.ViewHolder, ItemSoundBinding>(Companion),
        ItemDragHelperAdapter<SoundViewModel>,
        ViewModelStoreOwner {
    private val viewModelStore = ViewModelStore()
    private val activity by lazy { fragment.requireActivity() as EditSoundInterface }

    companion object : DiffUtil.ItemCallback<SoundViewModel>() {
        override fun areItemsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SoundViewModel, newItem: SoundViewModel) =
                oldItem.uri == newItem.uri && oldItem.name == newItem.name && oldItem.order == newItem.order
    }

    override fun createViewHolder(binding: ItemSoundBinding, parent: ViewGroup) = ViewHolder(binding, parent.context)

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemSoundBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun bind(holder: ViewHolder, item: SoundViewModel) {
        Log.d(GlobalApplication.LOG_TAG, "SoundAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.bind(item)

    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    override fun onItemsReordered() = fragment.soundListViewModel.updateOrder(currentList)

    override fun getMutableList(): MutableList<SoundViewModel> = currentList.toMutableList()

    override fun getViewModelStore() = viewModelStore


    inner class ViewHolder(binding: ItemSoundBinding, private val context: Context) :
            DataBoundViewHolder<ItemSoundBinding>(binding),
            View.OnClickListener,
            View.OnLongClickListener,
            PopupMenu.OnMenuItemClickListener,
            AppViewModelListenerInterface {
        private lateinit var viewModel: SoundViewModel
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.soundCard.setOnClickListener(this)
            binding.soundCard.setOnLongClickListener(this)
        }

        fun bind(item: SoundViewModel) {
            Log.i(GlobalApplication.LOG_TAG,
                    "SoundAdapter.ViewHolder.bind: adapter ${this@SoundAdapter.hashCode()}, " +
                            "viewHolder ${hashCode()}, SoundViewModel $item")

            this.viewModel = item
            binding.viewModel = viewModel

            if (!viewModel.isValid)
                binding.failIcon.visibility = View.VISIBLE

            viewModel.isPlaying.observe(this, { onIsPlayingChange(it) })
            fragment.appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
        }

        private fun onIsPlayingChange(value: Boolean) {
            binding.playIcon.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun onReorderEnabledChange(value: Boolean) {
            if (value) binding.soundCard.setOnLongClickListener(null) else binding.soundCard.setOnLongClickListener(this)
        }

        override fun onZoomLevelChange(value: Int) = Unit

        override fun onLongClick(v: View?): Boolean {
            v?.let {
                PopupMenu(v.context, v).apply {
                    menuInflater.inflate(R.menu.sound_context_menu, menu)
                    setOnMenuItemClickListener(this@ViewHolder)
                    show()
                }
            }
            if (v is CardView)
                SoundItemLongClickAnimator(v).start()
            return true
        }

        override fun onClick(view: View?) {
            if (!viewModel.isValid) showErrorToast() else viewModel.playOrPause()

            view?.let {
                (AnimatorInflater.loadAnimator(context, R.animator.sound_item_click_animator) as AnimatorSet).apply {
                    setTarget(view)
                    start()
                }
            }
        }

        fun onItemSelected() = SoundItemLongClickAnimator(binding.soundCard).start()

        override fun toString() = super.toString() + " '" + binding.soundName.text + "'"

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            try {
                when (item?.itemId) {
                    R.id.sound_context_menu_edit ->
                        activity.showSoundEditDialog(viewModel.id!!, viewModel.categoryId!!)
                    R.id.sound_context_menu_delete ->
                        activity.showSoundDeleteDialog(viewModel.id!!, viewModel.name)
                }
            } catch (e: NullPointerException) {
                Toast.makeText(context, R.string.data_not_fetched_yet, Toast.LENGTH_SHORT).show()
            }
            return true
        }

        private fun showErrorToast() = Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()
    }
}