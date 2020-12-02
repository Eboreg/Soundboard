package us.huseli.soundboard_kotlin.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.animators.CollapseButtonAnimator
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.databinding.ItemCategoryBinding
import us.huseli.soundboard_kotlin.helpers.CategoryItemDragHelperCallback
import us.huseli.soundboard_kotlin.helpers.SoundDragListener
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.interfaces.ToastInterface
import us.huseli.soundboard_kotlin.viewmodels.*

class CategoryAdapter(
        private val appViewModel: AppViewModel,
        private val initialSpanCount: Int,
        private val soundViewModel: SoundViewModel,
        private val categoryListViewModel: CategoryListViewModel,
        private val viewModelStoreOwner: ViewModelStoreOwner) :
        DataBoundAdapter<Category, CategoryAdapter.CategoryViewHolder, ItemCategoryBinding>(DiffCallback()) {
    private val soundViewPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 200) }
    internal val itemTouchHelper = ItemTouchHelper(CategoryItemDragHelperCallback())

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun createViewHolder(binding: ItemCategoryBinding, parent: ViewGroup) = CategoryViewHolder(binding)

    @SuppressLint("ClickableViewAccessibility")
    override fun bind(holder: CategoryViewHolder, item: Category, position: Int) {
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

    fun onItemsReordered() {
        currentList.forEachIndexed { index, item -> item.order = index }
        categoryListViewModel.saveOrder(currentList)
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
    inner class CategoryViewHolder(binding: ItemCategoryBinding) :
            DataBoundViewHolder<ItemCategoryBinding, Category>(binding),
            View.OnClickListener,
            AppViewModelListenerInterface /*,
            ViewModelStoreOwner */ {
        @Suppress("PrivatePropertyName")
        private val LOG_TAG = "CategoryViewHolder"

        // private val viewModelStore = ViewModelStore()
        private val collapseButtonAnimator = CollapseButtonAnimator(binding.categoryCollapseButton)

        private var category: Category? = null
        private var soundAdapter: SoundAdapter? = null
        private var soundCount: Int? = null

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)

            binding.soundList.apply {
                layoutManager = GridLayoutManager(context, initialSpanCount).also { lm ->
                    appViewModel.spanCount.observe(this@CategoryViewHolder) { lm.spanCount = it }
                }
                setRecycledViewPool(soundViewPool)
                setItemViewCacheSize(20)
            }
        }

        fun bind(category: Category) {
            Log.i(LOG_TAG, "ViewHolder.bind: ${this@CategoryAdapter.hashCode()} ViewHolder ${hashCode()} " +
                    "bind Category ${category.name} (${category.hashCode()})")

            this.category = category

            // TODO: Make more failsafe
            val viewModelFactory = CategoryViewModelFactory(category.id!!)
            val categoryViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory).get(
                    category.id.toString(), CategoryViewModel::class.java)
            val soundAdapter = SoundAdapter(categoryViewModel, binding.soundList, soundViewModel, appViewModel).also { soundAdapter = it }
            val soundDragListener = SoundDragListener(soundAdapter, this)

            binding.categoryViewModel = categoryViewModel
            binding.root.setOnDragListener(soundDragListener)
            binding.soundList.adapter = soundAdapter

            categoryViewModel.backgroundColor.observe(this) { color -> binding.categoryHeader.setBackgroundColor(color) }
            categoryViewModel.collapsed.observe(this) { collapsed ->
                if (!soundDragListener.isDragging) soundDragListener.wasCollapsed = collapsed
                collapseButtonAnimator.animate(collapsed)
                binding.soundList.visibility = if (collapsed) View.GONE else View.VISIBLE
            }

            soundViewModel.getByCategory(category.id).observe(this) { sounds ->
                Log.i(LOG_TAG, "ViewHolder sound list observer: adapter=${this@CategoryAdapter}, viewHolder=$this, category=$category, sounds=$sounds")

                // TODO: remove, this is test data
                /*
                val invalidSound = Sound(666, category.id, "fail", Uri.fromParts("content", "//com.android.externalstorage.documents/document/0000-0000:Music/Soundboard/Uh! Sorry!.flac", null), 10, 100)
                val mutableSounds = sounds.toMutableList() as MutableList<AbstractSound>
                mutableSounds.add(invalidSound)
                soundCount = mutableSounds.count()
                soundAdapter.submitList(mutableSounds)
                 */

                soundCount = sounds.count().also {
                    if (it > 20) binding.soundList.setItemViewCacheSize(it)
                }
                soundAdapter.submitList(sounds)
            }
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

        //override fun getViewModelStore() = viewModelStore

        override fun markDestroyed() {
            // Fragment calls adapter.setLifecycleDestroyed(), which calls this
            // We need to pass it on to soundAdapter
            super.markDestroyed()
            soundAdapter?.setLifecycleDestroyed()
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "CategoryAdapter.ViewHolder $hashCode <adapterPosition=$adapterPosition, category=$category>"
        }
    }

    companion object {
        const val LOG_TAG = "CategoryAdapter"
    }
}