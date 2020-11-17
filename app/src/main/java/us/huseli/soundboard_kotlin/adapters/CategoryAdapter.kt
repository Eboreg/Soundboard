package us.huseli.soundboard_kotlin.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import us.huseli.soundboard_kotlin.helpers.SoundDragListener2
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.interfaces.ToastInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel

class CategoryAdapter(
        private val activity: FragmentActivity, private val appViewModel: AppViewModel, private val initialSpanCount: Int) :
        DataBoundAdapter<Category, CategoryAdapter.ViewHolder, ItemCategoryBinding>(DiffCallback()) {
    private val soundViewPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 20) }
    internal val itemTouchHelper = ItemTouchHelper(CategoryItemDragHelperCallback())
//    public override val currentList: List<Category>
//        get() = super.currentList

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun createViewHolder(binding: ItemCategoryBinding, parent: ViewGroup) = ViewHolder(binding)

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(holder: ViewHolder, item: Category, position: Int) {
        holder.binding.categoryMoveButton.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) itemTouchHelper.startDrag(holder)
            return@setOnTouchListener false
        }
        holder.bind(item)
    }

    override fun getItemById(id: Int) = currentList.find { it.id == id }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "CategoryAdapter $hashCode"
    }


    class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.name == newItem.name && oldItem.backgroundColor == newItem.backgroundColor
        }
    }


    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    inner class ViewHolder(binding: ItemCategoryBinding) :
            DataBoundViewHolder<ItemCategoryBinding, Category>(binding),
            View.OnClickListener,
            AppViewModelListenerInterface,
            ViewModelStoreOwner {
        private val categoryViewModel = CategoryViewModel()
        private val viewModelStore = ViewModelStore()
        // private val soundDragListener = SoundDragListener(this)
        private val collapseButtonAnimator = CollapseButtonAnimator(binding.categoryCollapseButton)

        private val soundAdapter = SoundAdapter(activity, appViewModel, categoryViewModel, binding.soundList)
        private val soundDragListener = SoundDragListener2(soundAdapter, this)

        private var category: Category? = null
        private var soundCount: Int? = null

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)
            binding.root.setOnDragListener(soundDragListener)

            binding.soundList.apply {
                adapter = soundAdapter
                layoutManager = GridLayoutManager(context, initialSpanCount).also { lm ->
                    appViewModel.spanCount.observe(this@ViewHolder) { lm.spanCount = it }
                }
                setRecycledViewPool(soundViewPool)
            }
            categoryViewModel.sounds.observe(this) { sounds ->
                Log.i(GlobalApplication.LOG_TAG,
                        "${this@CategoryAdapter}: viewholder=$this, " +
                                "recyclerView ${binding.soundList.hashCode()}, " +
                                "SoundAdapter $soundAdapter, " +
                                "Category $category, " +
                                "sounds changed: $sounds")
                soundCount = sounds.count()
                soundAdapter.submitList(sounds)
            }
            categoryViewModel.backgroundColor.observe(this) { color -> binding.categoryHeader.setBackgroundColor(color) }
            categoryViewModel.collapsed.observe(this) { collapsed ->
                if (!soundDragListener.isDragging) soundDragListener.wasCollapsed = collapsed
                collapseButtonAnimator.animate(collapsed)
                binding.soundList.visibility = if (collapsed) View.GONE else View.VISIBLE
            }
        }

        fun bind(category: Category) {
            Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter.bind: ${this@CategoryAdapter.hashCode()} ViewHolder ${hashCode()} " +
                    "bind Category ${category.name} (${category.hashCode()}), categoryViewModel ${categoryViewModel.hashCode()}")

            this.category = category

            category.id?.let { soundAdapter.createEmptySound(it) }
            categoryViewModel.setCategory(category)
            binding.categoryViewModel = categoryViewModel
        }

        fun showDropContainer() {
            binding.emptyCategoryDropContainer.visibility = View.VISIBLE
        }

        fun hideDropContainer() {
            binding.emptyCategoryDropContainer.visibility = View.GONE
        }

        fun getYOffset() = binding.soundList.y

        override fun onClick(v: View?) {
            // When icons in the category header are clicked
            val activity = binding.root.context as EditCategoryInterface
            category?.also { category ->
                category.id?.also { catId ->
                    when (v) {
                        binding.categoryEditButton -> activity.showCategoryEditDialog(catId)
                        binding.categoryDeleteButton -> activity.showCategoryDeleteDialog(catId, category.name, soundCount ?: 0)
                    }
                }
            } ?: run {
                (activity as ToastInterface).showToast(R.string.not_initialized_yet)
            }
        }

        override fun onReorderEnabledChange(value: Boolean) {}

        override fun onSelectEnabledChange(value: Boolean) {}

        override fun getViewModelStore() = viewModelStore

        override fun markDestroyed() {
            // Fragment calls adapter.setLifecycleDestroyed(), which calls this
            // We need to pass it on to soundAdapter
            super.markDestroyed()
            soundAdapter.setLifecycleDestroyed()
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "CategoryAdapter.ViewHolder $hashCode <adapterPosition=$adapterPosition, category=$category>"
        }
    }
}