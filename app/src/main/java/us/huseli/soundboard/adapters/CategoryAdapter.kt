package us.huseli.soundboard.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.R
import us.huseli.soundboard.adapters.common.LifecycleAdapter
import us.huseli.soundboard.adapters.common.LifecycleViewHolder
import us.huseli.soundboard.animators.CollapseButtonAnimator
import us.huseli.soundboard.data.Category
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.databinding.ItemCategoryBinding
import us.huseli.soundboard.helpers.SoundDragListener
import us.huseli.soundboard.helpers.SoundScroller
import us.huseli.soundboard.interfaces.EditCategoryInterface
import us.huseli.soundboard.interfaces.SnackbarInterface
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.CategoryViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel

class CategoryAdapter(
    private val appViewModel: AppViewModel,
    private val soundViewModel: SoundViewModel,
    private val categoryViewModel: CategoryViewModel,
    private val activity: FragmentActivity,
    private val soundScroller: SoundScroller
) : LifecycleAdapter<Category, CategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val item = getItem(position)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "onBindViewHolder: item=$item, holder=$holder, position=$position, adapter=$this")
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = CategoryViewHolder(
            binding,
            activity,
            categoryViewModel,
            soundViewModel,
            appViewModel,
            soundScroller,
        )
        binding.lifecycleOwner = holder

        return holder
    }

    override fun onCurrentListChanged(previousList: MutableList<Category>, currentList: MutableList<Category>) {
        super.onCurrentListChanged(previousList, currentList)
        viewHolders.forEach { it.setupMoveButtons() }
    }

    override fun toString(): String {
        val hashCode = Integer.toHexString(System.identityHashCode(this))
        return "CategoryAdapter $hashCode"
    }


    companion object {
        const val LOG_TAG = "CategoryAdapter"
    }


    class DiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem

        override fun areContentsTheSame(oldItem: Category, newItem: Category) =
            oldItem.name == newItem.name &&
                    oldItem.backgroundColor == newItem.backgroundColor &&
                    oldItem.collapsed == newItem.collapsed
    }


    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    class CategoryViewHolder(
        internal val binding: ItemCategoryBinding,
        private val activity: FragmentActivity,
        private val categoryViewModel: CategoryViewModel,
        private val soundViewModel: SoundViewModel,
        appViewModel: AppViewModel,
        soundScroller: SoundScroller,
    ) :
        View.OnClickListener,
        LifecycleViewHolder<Category>(binding.root) {

        private val collapseButtonAnimator = CollapseButtonAnimator(binding.categoryCollapseButton)
        private val soundDragListener: SoundDragListener

        private var isCollapsed: Boolean? = null
        private var soundCount: Int? = null

        private val soundAdapter = SoundAdapter(
            soundViewModel,
            appViewModel,
            categoryViewModel
        )

        override var item: Category? = null
        override val lifecycleRegistry = LifecycleRegistry(this)

        init {
            val soundLayoutManager = SoundLayoutManager(binding.root.context, appViewModel.spanCount.value)
            soundDragListener = SoundDragListener(soundAdapter, this, soundScroller)

            enableClickAndTouch()
            setupMoveButtons()
            binding.root.setOnDragListener(soundDragListener)

            appViewModel.spanCount.observe(this) {
                if (it != null) soundLayoutManager.spanCount = it
            }

            binding.soundList.adapter = soundAdapter
            binding.soundList.layoutManager = soundLayoutManager

            appViewModel.reorderEnabled.observe(this) { onReorderEnabledChange(it) }
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
            soundAdapter.categoryId = categoryId

            onCollapseChanged(category.collapsed)

            if (category.collapsed) binding.loadingBar.visibility = View.GONE

            soundViewModel.filteredSounds.observe(this) { allSounds ->
                val sounds = allSounds.filter { it.categoryId == category.id }
                soundCount = sounds.size.also {
                    if (it > 20) binding.soundList.setItemViewCacheSize(it)
                }
                soundAdapter.submitList(sounds)
            }

            soundViewModel.selectEnabled.observe(this) { onSelectEnabledChange(it) }
        }

        internal fun setupMoveButtons() {
            if (bindingAdapterPosition > 0) enableClickAndTouch(binding.categoryMoveUp) else disableClickAndTouch(
                binding.categoryMoveUp)
            bindingAdapter?.also {
                if (bindingAdapterPosition < it.itemCount - 1) enableClickAndTouch(binding.categoryMoveDown) else disableClickAndTouch(
                    binding.categoryMoveDown)
            }
        }

        internal fun getYOffset() = binding.soundList.y

        internal fun hideDropContainer() {
            binding.emptyCategoryDropContainer.visibility = View.GONE
        }

        internal fun showDropContainer() {
            binding.emptyCategoryDropContainer.visibility = View.VISIBLE
        }


        /********* PRIVATE METHODS **********/
        private fun disableClickAndTouch(view: View) {
            view.setOnClickListener(null)
            view.alpha = 0.5f
            view.isClickable = false
        }

        private fun disableClickAndTouch() =
            listOf(binding.categoryEditButton,
                binding.categoryDeleteButton,
                binding.categoryMoveDown,
                binding.categoryMoveUp).forEach { disableClickAndTouch(it) }

        private fun enableClickAndTouch(view: View) {
            view.setOnClickListener(this)
            view.alpha = 1.0f
            view.isClickable = true
        }

        private fun enableClickAndTouch() =
            listOf(binding.categoryEditButton,
                binding.categoryDeleteButton,
                binding.categoryCollapse).forEach { enableClickAndTouch(it) }

        private fun onCollapseChanged(value: Boolean) {
            if (!soundDragListener.isDragging) soundDragListener.wasCollapsed = value
            binding.soundList.visibility = if (value) View.GONE else View.VISIBLE
            isCollapsed = value
        }

        private fun onReorderEnabledChange(value: Boolean?) {
            if (value == true) binding.categoryMoveButtons.visibility = View.VISIBLE
            else binding.categoryMoveButtons.visibility = View.GONE
        }

        private fun onSelectEnabledChange(value: Boolean) {
            if (value) disableClickAndTouch()
            else {
                enableClickAndTouch()
                setupMoveButtons()
            }
        }

        private fun toggleCollapsed() {
            item?.let { category ->
                val collapsed = !category.collapsed
                collapseButtonAnimator.animate(collapsed)
                category.id?.let { categoryViewModel.setCollapsed(it, collapsed) }
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
                        binding.categoryEditButton -> activity.showCategoryEditDialog(category)
                        binding.categoryDeleteButton -> activity.showCategoryDeleteDialog(
                            catId, category.name, soundCount ?: 0)
                        binding.categoryCollapse -> toggleCollapsed()
                        binding.categoryMoveDown -> moveCategoryDown()
                        binding.categoryMoveUp -> moveCategoryUp()
                    }
                }
            } ?: run {
                (this.activity as SnackbarInterface).showSnackbar(R.string.not_initialized_yet)
            }
        }

        private fun moveCategoryUp() {
            if (bindingAdapterPosition > 0) {
                categoryViewModel.swap(bindingAdapterPosition, bindingAdapterPosition - 1)
            }
        }

        private fun moveCategoryDown() {
            bindingAdapter?.also { adapter ->
                if (bindingAdapterPosition < adapter.itemCount - 1) {
                    categoryViewModel.swap(bindingAdapterPosition, bindingAdapterPosition + 1)
                }
            }
        }

        override fun toString(): String {
            val hashCode = Integer.toHexString(System.identityHashCode(this))
            return "CategoryAdapter.ViewHolder $hashCode <adapterPosition=$bindingAdapterPosition, category=$item>"
        }


        companion object {
            const val LOG_TAG = "CategoryViewHolder"
        }


        inner class SoundLayoutManager(context: Context, spanCount: Int?) :
            GridLayoutManager(context, spanCount ?: Constants.DEFAULT_SPANCOUNT_LANDSCAPE),
            ItemTouchHelper.ViewDropHandler {

            override fun isAutoMeasureEnabled() = true

            override fun prepareForDrop(view: View, target: View, x: Int, y: Int) {
                binding.categoryItem.setBackgroundColor(ResourcesCompat.getColor(activity.resources,
                    android.R.color.transparent,
                    null))
                binding.categoryItem.translationZ = 0f
                super.prepareForDrop(view, target, x, y)
            }

// TODO: Graphically buggy bastard, maybe try again with something else someday
/*
            override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                super.onLayoutChildren(recycler, state)
                binding.loadingBar.visibility = when {
                    isCollapsed == true || itemCount == 0 -> View.GONE
                    state != null && (state.isPreLayout || state.isMeasuring) -> View.VISIBLE
                    else -> View.GONE
                }
                if (BuildConfig.DEBUG)
                    Log.d(LOG_TAG,
                        "onLayoutChildren: category=$item, itemCount=$itemCount, state=$state = loadingBar.visibility=" + if (binding.loadingBar.visibility == View.GONE) "GONE" else "VISIBLE")
            }
*/
        }
    }
}