package us.huseli.soundboard.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.R
import us.huseli.soundboard.adapters.common.LifecycleAdapter
import us.huseli.soundboard.adapters.common.LifecycleViewHolder
import us.huseli.soundboard.animators.CollapseButtonAnimator
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.data.SoundWithCategory
import us.huseli.soundboard.databinding.ItemCategoryBinding
import us.huseli.soundboard.helpers.CategoryItemDragHelperCallback
import us.huseli.soundboard.helpers.SoundDragListener
import us.huseli.soundboard.helpers.SoundScroller
import us.huseli.soundboard.interfaces.EditCategoryInterface
import us.huseli.soundboard.interfaces.SnackbarInterface
import us.huseli.soundboard.viewmodels.*
import java.util.*

class CategoryAdapter(
    private val appViewModel: AppViewModel,
    private val initialSpanCount: Int,
    private val soundViewModel: SoundViewModel,
    private val categoryViewModel: CategoryViewModel,
    private val activity: FragmentActivity,
    private val soundScroller: SoundScroller
) :
    LifecycleAdapter<Category, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {
    internal val itemTouchHelper = ItemTouchHelper(CategoryItemDragHelperCallback())

    override val firstVisibleViewHolder: CategoryViewHolder?
        get() = viewHolders.firstOrNull { it.isVisible() && it.soundAdapter.isNotEmpty() }

    override val lastVisibleViewHolder: CategoryViewHolder?
        get() = viewHolders.lastOrNull { it.isVisible() && it.soundAdapter.isNotEmpty() }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "onBindViewHolder: item=$item, holder=$holder, position=$position, adapter=$this")
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = CategoryViewHolder(binding, this)
        binding.lifecycleOwner = holder

        return holder
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "CategoryAdapter $hashCode"
    }

    fun onItemsReordered() {
        val previousOrder = currentList.map { it.order }
        currentList.forEachIndexed { index, item -> item.order = index }
        val newOrder = currentList.map { it.order }
        if (previousOrder != newOrder) {
            appViewModel.pushCategoryUndoState(activity)
            categoryViewModel.saveOrder(currentList)
        }
    }


    companion object {
        const val LOG_TAG = "CategoryAdapter"
    }


    class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.name == newItem.name && oldItem.backgroundColor == newItem.backgroundColor && oldItem.collapsed == newItem.collapsed
        }
    }


    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    class CategoryViewHolder(internal val binding: ItemCategoryBinding, adapter: CategoryAdapter) :
        View.OnClickListener,
        View.OnTouchListener,
        LifecycleViewHolder<Category>(binding.root) {
        private val activity = adapter.activity
        private val appViewModel = adapter.appViewModel
        private val categoryListViewModel = adapter.categoryViewModel
        private val collapseButtonAnimator = CollapseButtonAnimator(binding.categoryCollapseButton)
        private val initialSpanCount = adapter.initialSpanCount
        private val itemTouchHelper = adapter.itemTouchHelper
        private val soundDragListener: SoundDragListener
        private val soundScroller = adapter.soundScroller
        private val soundViewModel = adapter.soundViewModel

        private var isCollapsed: Boolean? = null
        private var soundCount: Int? = null

        internal val soundAdapter: SoundAdapter = SoundAdapter(
            binding.soundList,
            soundViewModel,
            appViewModel,
            categoryListViewModel,
            activity
        )

        override var item: Category? = null
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            soundDragListener = SoundDragListener(soundAdapter, this, soundScroller)

            enableClickAndTouch()
            binding.root.setOnDragListener(soundDragListener)

            binding.soundList.apply {
                this.adapter = soundAdapter
                layoutManager = SoundLayoutManager(context, initialSpanCount).also { lm ->
                    appViewModel.spanCount.observe(this@CategoryViewHolder) {
                        if (it != null) lm.spanCount = it
                    }
                }
                isNestedScrollingEnabled = false

                // TODO: Does this actually work as intended?
                viewTreeObserver.addOnGlobalLayoutListener {
                    (layoutManager as SoundLayoutManager).let {
                        val itemsShown =
                            it.findLastVisibleItemPosition() - it.findFirstVisibleItemPosition() + 1
                        binding.loadingBar.visibility =
                            if (isCollapsed == true || itemsShown >= it.itemCount) View.GONE else View.VISIBLE
                    }
                }
            }
        }

        /********* PUBLIC/INTERNAL METHODS **********/
        internal fun bind(category: Category) {
            item = category
            val categoryId = category.id
            if (categoryId == null) {
                Log.e(LOG_TAG, "bind: got Category with id==null")
                return
            }

            binding.category = category
            soundAdapter.category = category

            onCollapseChanged(category.collapsed)

            if (category.collapsed) binding.loadingBar.visibility = View.GONE

            binding.categoryHeader.setBackgroundColor(category.backgroundColor)
            soundViewModel.filteredSounds.observe(this) { allSounds ->
                val sounds = allSounds.filter { it.category == category }
                // Log.d(LOG_TAG, "ViewHolder sound list observer: viewHolder=$this, category=$category, sounds=$sounds")
                // TODO: Remove test call + method when not needed
                // submitListWithInvalidSound(sounds)

                soundCount = sounds.size.also {
                    if (it > 20) binding.soundList.setItemViewCacheSize(it)
                }
                soundAdapter.submitList(sounds)
            }

            soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }
        }

        internal fun getYOffset() = binding.soundList.y

        internal fun hideDropContainer() {
            binding.emptyCategoryDropContainer.visibility = View.GONE
        }

        internal fun showDropContainer() {
            binding.emptyCategoryDropContainer.visibility = View.VISIBLE
        }


        /********* PRIVATE METHODS **********/
        @SuppressLint("ClickableViewAccessibility")
        private fun disableClickAndTouch() {
            listOf(
                binding.categoryEditButton,
                binding.categoryDeleteButton,
                binding.categorySortButton,
            ).forEach {
                it.setOnClickListener(null)
                it.alpha = 0.5f
                it.isClickable = false
            }
            binding.categoryMoveButton.setOnTouchListener(null)
            binding.categoryMoveButton.alpha = 0.3f
            binding.categoryMoveButton.isClickable = false
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun enableClickAndTouch() {
            listOf(
                binding.categoryEditButton,
                binding.categoryDeleteButton,
                binding.categorySortButton,
                binding.categoryCollapse,
            ).forEach {
                it.setOnClickListener(this)
                it.alpha = 1.0f
                it.isClickable = true
            }
            binding.categoryMoveButton.setOnTouchListener(this)
            binding.categoryMoveButton.alpha = 1.0f
            binding.categoryMoveButton.isClickable = true
        }

        private fun onCollapseChanged(value: Boolean) {
            if (!soundDragListener.isDragging) soundDragListener.wasCollapsed = value
            binding.soundList.visibility = if (value) View.GONE else View.VISIBLE
            isCollapsed = value
        }

        private fun onSelectEnabledChange(value: Boolean) {
            soundAdapter.onSelectEnabledChange(value)
            if (value) disableClickAndTouch()
            else enableClickAndTouch()
        }

        @Suppress("unused")
        private fun submitListWithInvalidSound(soundsWithCategory: List<SoundWithCategory>) {
            val uri = Uri.fromParts(
                "content",
                "//com.android.externalstorage.documents/document/0000-0000:Music/Soundboard/Uh! Sorry!.flac",
                null
            )
            val invalidSound =
                Sound(666, item?.id, "fail", uri.path!!, 10, 100, Date(), -1, null)
            val invalidSoundWithCategory = SoundWithCategory(invalidSound, item!!)
            val mutableSounds = soundsWithCategory.toMutableList()
            mutableSounds.add(invalidSoundWithCategory)
            soundCount = mutableSounds.count()
            soundAdapter.submitList(mutableSounds)
        }

        private fun toggleCollapsed() {
            item?.let { category ->
                val collapsed = !category.collapsed
                collapseButtonAnimator.animate(collapsed)
                // onCollapseChanged(collapsed)
                category.id?.let { categoryListViewModel.setCollapsed(it, collapsed) }
            }
        }


        /********* OVERRIDDEN METHODS **********/
        override fun markDestroyed() {
            // Fragment calls adapter.setLifecycleDestroyed(), which calls this
            // We need to pass it on to soundAdapter
            super.markDestroyed()
            soundAdapter.setLifecycleDestroyed()
        }

        override fun onClick(v: View?) {
            // When icons in the category header are clicked
            val activity = activity as EditCategoryInterface
            item?.also { category ->
                category.id?.also { catId ->
                    when (v) {
                        binding.categoryEditButton -> activity.showCategoryEditDialog(catId)
                        binding.categoryDeleteButton -> activity.showCategoryDeleteDialog(
                            catId, category.name, soundCount ?: 0)
                        binding.categorySortButton -> activity.showCategorySortDialog(catId, category.name)
                        binding.categoryCollapse -> toggleCollapsed()
                    }
                }
            } ?: run {
                (this.activity as SnackbarInterface).showSnackbar(R.string.not_initialized_yet)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN && v == binding.categoryMoveButton)
                itemTouchHelper.startDrag(this)
            return false
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "CategoryAdapter.ViewHolder $hashCode <adapterPosition=$bindingAdapterPosition, category=$item>"
        }


        companion object {
            const val LOG_TAG = "CategoryViewHolder"
        }


        inner class SoundLayoutManager(context: Context, spanCount: Int) :
            GridLayoutManager(context, spanCount) {
            override fun isAutoMeasureEnabled() = true
        }
    }
}