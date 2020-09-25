package us.huseli.soundboard_kotlin.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.CollapseButtonAnimator
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.databinding.ItemCategoryBinding
import us.huseli.soundboard_kotlin.helpers.CategoryItemDragHelperCallback
import us.huseli.soundboard_kotlin.helpers.SoundItemDragHelperCallback
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.interfaces.ItemDragHelperAdapter
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel

class CategoryAdapter(
        private val activity: FragmentActivity, private val categoryListViewModel: CategoryListViewModel,
        private val appViewModel: AppViewModel, private val initialSpanCount: Int) :
        DataBoundAdapter<Category, CategoryAdapter.ViewHolder, ItemCategoryBinding>(),
        ItemDragHelperAdapter<Category> {
    private val soundViewPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 20) }
    internal val itemTouchHelper = ItemTouchHelper(CategoryItemDragHelperCallback())
    override val currentList = mutableListOf<Category>()

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun createViewHolder(binding: ItemCategoryBinding, parent: ViewGroup) = ViewHolder(binding)

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(holder: ViewHolder, item: Category) {
        holder.binding.categoryMoveButton.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) itemTouchHelper.startDrag(holder)
            return@setOnTouchListener false
        }
        holder.bind(item)
    }

    override fun onItemsReordered() = categoryListViewModel.saveOrder(currentList)

    override fun calculateDiff(list: List<Category>) = DiffUtil.calculateDiff(DiffCallback(list, currentList), true).dispatchUpdatesTo(this)


    inner class DiffCallback(newRows: List<Category>, oldRows: List<Category>) :
            DataBoundAdapter<Category, ViewHolder, ItemCategoryBinding>.DiffCallback(newRows, oldRows) {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem.name == newItem.name && oldItem.backgroundColor == newItem.backgroundColor
    }

    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    inner class ViewHolder(binding: ItemCategoryBinding) :
            DataBoundViewHolder<ItemCategoryBinding>(binding),
            View.OnClickListener,
            AppViewModelListenerInterface,
            ViewModelStoreOwner {
        private val soundItemTouchHelper = ItemTouchHelper(SoundItemDragHelperCallback())
        private val categoryViewModel = CategoryViewModel()
        private val viewModelStore = ViewModelStore()
        private val soundAdapter = SoundAdapter(activity, appViewModel).apply {
            setOnItemsReordered { sounds -> categoryViewModel.updateSoundOrder(sounds) }
        }
        private val collapseButtonAnimator = CollapseButtonAnimator(binding.categoryCollapseButton)
        private lateinit var category: Category
        private var soundCount: Int? = null

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)
            binding.categoryCollapseButton.setOnClickListener(this)
            binding.soundList.apply {
                adapter = soundAdapter
                layoutManager = GridLayoutManager(context, initialSpanCount).also { lm ->
                    appViewModel.spanCount.observe(this@ViewHolder, { lm.spanCount = it })
                }
                setRecycledViewPool(soundViewPool)
            }
            categoryViewModel.sounds.observe(this, { sounds ->
                Log.i(GlobalApplication.LOG_TAG,
                        "CategoryAdapter ${this@CategoryAdapter.hashCode()}: viewholder ${hashCode()}, " +
                                "recyclerView ${binding.soundList.hashCode()}, " +
                                "SoundAdapter ${soundAdapter.hashCode()}, " +
                                "Category ${category.id} ${category.name}, " +
                                "sounds changed: $sounds")
                soundCount = sounds.count()
                soundAdapter.submitList(sounds.toMutableList())
            })
            categoryViewModel.backgroundColor.observe(this, { color -> binding.categoryHeader.setBackgroundColor(color) })
            categoryViewModel.collapsed.observe(this, { collapsed ->
                collapseButtonAnimator.animate(collapsed)
                binding.soundList.visibility = if (collapsed) View.GONE else View.VISIBLE
            })

            appViewModel.reorderEnabled.observe(this, { value -> onReorderEnabledChange(value) })
        }

        fun bind(category: Category) {
            Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter.bind: ${this@CategoryAdapter.hashCode()} ViewHolder ${hashCode()} " +
                    "bind Category ${category.name} (${category.hashCode()}), categoryViewModel ${categoryViewModel.hashCode()}")

            this.category = category

            categoryViewModel.setCategory(category)
            binding.categoryViewModel = categoryViewModel
        }

        override fun onClick(v: View?) {
            // When icons in the category header are clicked
            val activity = binding.root.context as EditCategoryInterface
            try {
                val categoryId = category.id!!
                when (v) {
                    binding.categoryCollapseButton -> categoryViewModel.toggleCollapsed()
                    binding.categoryEditButton -> activity.showCategoryEditDialog(categoryId)
                    binding.categoryDeleteButton -> activity.showCategoryDeleteDialog(categoryId, category.name, soundCount ?: 0)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CategoryAdapter.activity, R.string.not_initialized_yet, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onReorderEnabledChange(value: Boolean) =
                if (value) soundItemTouchHelper.attachToRecyclerView(binding.soundList) else soundItemTouchHelper.attachToRecyclerView(null)

        override fun onSelectEnabledChange(value: Boolean) {}

        override fun getViewModelStore() = viewModelStore
    }
}