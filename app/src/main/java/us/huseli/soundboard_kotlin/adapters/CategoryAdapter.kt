package us.huseli.soundboard_kotlin.adapters

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.data.CategoryWithSounds
import us.huseli.soundboard_kotlin.databinding.ItemCategoryBinding
import us.huseli.soundboard_kotlin.helpers.SoundItemDragHelperCallback
import us.huseli.soundboard_kotlin.interfaces.*
import us.huseli.soundboard_kotlin.viewmodels.*

class CategoryAdapter(private val fragment: Fragment, private val categoryListViewModel: CategoryListViewModel, private val appViewModel: AppViewModel) :
        DataBoundListAdapter<CategoryWithSounds, CategoryAdapter.ViewHolder, ItemCategoryBinding>(Companion),
        ItemDragHelperAdapter<CategoryWithSounds> {
    private val soundViewPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 20) }

    companion object : DiffUtil.ItemCallback<CategoryWithSounds>() {
        override fun areItemsTheSame(oldItem: CategoryWithSounds, newItem: CategoryWithSounds) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CategoryWithSounds, newItem: CategoryWithSounds): Boolean {
            return oldItem.name == newItem.name && oldItem.backgroundColor == newItem.backgroundColor && oldItem.order == newItem.order
        }
    }

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun createViewHolder(binding: ItemCategoryBinding, parent: ViewGroup) = ViewHolder(binding)

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(holder: ViewHolder, item: CategoryWithSounds) {
        Log.d(GlobalApplication.LOG_TAG, "CategoryAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.binding.categoryMoveButton.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) (fragment as StartDragListenerInterface).onStartDrag(holder)
            return@setOnTouchListener false
        }
        holder.bind(item)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemsReordered(newList: MutableList<CategoryWithSounds>) {
        categoryListViewModel.updateOrder(newList)
    }

    override fun getMutableList(): MutableList<CategoryWithSounds> = currentList.toMutableList()


    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    inner class ViewHolder(binding: ItemCategoryBinding) :
            DataBoundViewHolder<ItemCategoryBinding>(binding),
            View.OnClickListener,
            AppViewModelListenerInterface,
            ViewModelStoreOwner {
        private lateinit var categoryViewModel: CategoryViewModel
        private lateinit var soundAdapter: SoundAdapter
        private lateinit var soundItemTouchHelper: ItemTouchHelper
        private lateinit var soundListViewModel: SoundListViewModel
        private val viewModelStore = ViewModelStore()

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)
            binding.categoryMoveButton.setOnClickListener(this)
        }

        fun bind(category: CategoryWithSounds) {
            Log.d(GlobalApplication.LOG_TAG, "CategoryAdapter.ViewHolder ${hashCode()} bind Category ${category.hashCode()}")
            categoryViewModel = CategoryViewModel(category)
            //this.categoryViewModel = categoryViewModel
            binding.categoryViewModel = categoryViewModel

            soundListViewModel = ViewModelProvider(this, SoundListViewModelFactory(categoryViewModel.id!!)).get(SoundListViewModel::class.java)

            // Create "sub-adapter" SoundAdapter and do various bindings
            soundAdapter = SoundAdapter(fragment.requireActivity() as EditSoundInterface, appViewModel, soundListViewModel)
            soundItemTouchHelper = ItemTouchHelper(SoundItemDragHelperCallback(soundAdapter))

            binding.soundList.apply {
                adapter = soundAdapter
                layoutManager = GridLayoutManager(context, zoomLevelToSpanCount(0))
                setRecycledViewPool(soundViewPool)
            }

            soundListViewModel.sounds.observe(this, {
                Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter: soundListViewModel.sounds changed: $it")
                soundAdapter.submitList(it.toMutableList())
            })

            // Observe changes in zoomLevel and reorderEnabled
            appViewModel.zoomLevel.observe(this, { value -> onZoomLevelChange(value) })
            appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
        }

        override fun onClick(v: View?) {
            // When icons in the category header are clicked
            val activity = binding.root.context as EditCategoryInterface
            val categoryId = categoryViewModel.id!!
            when (v) {
                binding.categoryEditButton -> activity.showCategoryEditDialog(categoryId)
                binding.categoryDeleteButton -> activity.showCategoryDeleteDialog(categoryViewModel.id!!, categoryViewModel.name, categoryViewModel.soundCount)
            }
        }

        override fun onReorderEnabledChange(value: Boolean) =
                if (value) soundItemTouchHelper.attachToRecyclerView(binding.soundList) else soundItemTouchHelper.attachToRecyclerView(null)

        override fun onZoomLevelChange(value: Int) {
            Log.d(GlobalApplication.LOG_TAG, "CategoryAdapter.ViewHolder ${hashCode()} onZoomLevelChange: $value")
            (binding.soundList.layoutManager as GridLayoutManager).apply { spanCount = zoomLevelToSpanCount(value) }
        }

        private fun zoomLevelToSpanCount(zoomLevel: Int): Int {
            val config = binding.root.resources.configuration
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Zoomlevel 0 in portrait mode = 4 columns, which means max zoom = zoomlevel 3
                if (zoomLevel >= 3) return 1
                return 4 - zoomLevel
            } else {
                // Zoomlevel 0 in landscape mode = this number of columns:
                val zoomLevel0SpanCount: Int = 3 * (config.screenWidthDp / config.screenHeightDp)
                if (zoomLevel >= (zoomLevel0SpanCount - 1)) return 1
                return zoomLevel0SpanCount - zoomLevel
            }
        }

        override fun getViewModelStore() = viewModelStore
    }
}