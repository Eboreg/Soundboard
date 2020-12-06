package us.huseli.soundboard.adapters

import android.annotation.SuppressLint
import android.net.Uri
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
import us.huseli.soundboard.R
import us.huseli.soundboard.adapters.common.LifecycleAdapter
import us.huseli.soundboard.adapters.common.LifecycleViewHolder
import us.huseli.soundboard.animators.CollapseButtonAnimator
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.databinding.ItemCategoryBinding
import us.huseli.soundboard.helpers.CategoryItemDragHelperCallback
import us.huseli.soundboard.helpers.SoundDragListener
import us.huseli.soundboard.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard.interfaces.EditCategoryInterface
import us.huseli.soundboard.interfaces.ToastInterface
import us.huseli.soundboard.viewmodels.*

class CategoryAdapter(
        private val appViewModel: AppViewModel,
        private val initialSpanCount: Int,
        private val soundViewModel: SoundViewModel,
        private val categoryListViewModel: CategoryListViewModel,
        private val viewModelStoreOwner: ViewModelStoreOwner) :
        LifecycleAdapter<Category, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {
    private val soundViewPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 200) }
    internal val itemTouchHelper = ItemTouchHelper(CategoryItemDragHelperCallback())

    /**
     * Overridden methods
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position)
        Log.i(LOG_TAG, "onBindViewHolder: item=$item, holder=$holder, position=$position, adapter=$this")
        holder.binding.categoryMoveButton.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) itemTouchHelper.startDrag(holder)
            return@setOnTouchListener false
        }
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = CategoryViewHolder(binding, this)
        Log.i(LOG_TAG, "onCreateViewHolder: holder=$holder, adapter=$this")
        binding.lifecycleOwner = holder

        return holder
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "CategoryAdapter $hashCode"
    }

    /**
     * Own methods
     */
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
    class CategoryViewHolder(internal val binding: ItemCategoryBinding, private val adapter: CategoryAdapter) :
            View.OnClickListener,
            AppViewModelListenerInterface,
            LifecycleViewHolder(binding.root) {
        @Suppress("PrivatePropertyName")
        private val LOG_TAG = "CategoryViewHolder"

        private val appViewModel = adapter.appViewModel
        private val collapseButtonAnimator = CollapseButtonAnimator(binding.categoryCollapseButton)
        private val initialSpanCount = adapter.initialSpanCount
        private val soundAdapter: SoundAdapter
        private val soundDragListener: SoundDragListener
        private val soundViewModel = adapter.soundViewModel
        private val soundViewPool = adapter.soundViewPool
        private val viewModelStoreOwner = adapter.viewModelStoreOwner

        private var category: Category? = null
        private var isCollapsed: Boolean? = null
        private var soundCount: Int? = null

        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)

            soundAdapter = SoundAdapter(binding.soundList, soundViewModel, appViewModel)
            soundDragListener = SoundDragListener(soundAdapter, this)

            binding.root.setOnDragListener(soundDragListener)

            binding.soundList.apply {
                this.adapter = soundAdapter
                layoutManager = GridLayoutManager(context, initialSpanCount).also { lm ->
                    appViewModel.spanCount.observe(this@CategoryViewHolder) { lm.spanCount = it }
                }
                setRecycledViewPool(soundViewPool)
                setItemViewCacheSize(20)
                isNestedScrollingEnabled = false
            }
        }

        fun bind(category: Category) {
            Log.i(LOG_TAG, "ViewHolder.bind: ${adapter.hashCode()} ViewHolder ${hashCode()} " +
                    "bind Category ${category.name} (${category.hashCode()})")

            this.category = category
            val categoryId = category.id
            if (categoryId == null) {
                Log.e(LOG_TAG, "bind: got Category with id==null")
                return
            }

            val viewModelFactory = CategoryViewModelFactory(categoryId)
            val categoryViewModel = ViewModelProvider(viewModelStoreOwner, viewModelFactory).get(
                    category.id.toString(), CategoryViewModel::class.java)

            binding.categoryViewModel = categoryViewModel
            soundAdapter.categoryViewModel = categoryViewModel

            categoryViewModel.backgroundColor.observe(this) { color ->
                if (color != null) binding.categoryHeader.setBackgroundColor(color)
            }
            categoryViewModel.collapsed.observe(this) { collapsed ->
                // Without this if, unnecessary animations are triggered on all categories?!
                if (collapsed != isCollapsed) {
                    if (!soundDragListener.isDragging) soundDragListener.wasCollapsed = collapsed
                    collapseButtonAnimator.animate(collapsed)
                    binding.soundList.visibility = if (collapsed) View.GONE else View.VISIBLE
                    isCollapsed = collapsed
                }
            }

            soundViewModel.getByCategory(category.id).observe(this) { sounds ->
                Log.i(LOG_TAG, "ViewHolder sound list observer: adapter=$adapter, viewHolder=$this, category=$category, sounds=$sounds")

                // TODO: Remove test call + method when not needed
                // submitListWithInvalidSound(sounds)

                soundCount = sounds.count().also {
                    if (it > 20) binding.soundList.setItemViewCacheSize(it)
                }
                soundAdapter.submitList(sounds)
            }

            soundViewModel.selectEnabled.observe(this) { soundAdapter.onSelectEnabledChange(it) }
        }

        @Suppress("unused")
        private fun submitListWithInvalidSound(sounds: List<Sound>) {
            val invalidSound = Sound(666, category?.id, "fail", Uri.fromParts("content", "//com.android.externalstorage.documents/document/0000-0000:Music/Soundboard/Uh! Sorry!.flac", null), 10, 100)
            val mutableSounds = sounds.toMutableList()
            mutableSounds.add(invalidSound)
            soundCount = mutableSounds.count()
            soundAdapter.submitList(mutableSounds)
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

    companion object {
        const val LOG_TAG = "CategoryAdapter"
    }
}