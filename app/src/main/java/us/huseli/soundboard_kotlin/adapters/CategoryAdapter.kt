package us.huseli.soundboard_kotlin.adapters

import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.databinding.ItemCategoryBinding
import us.huseli.soundboard_kotlin.helpers.SoundItemTouchHelperCallback
import us.huseli.soundboard_kotlin.interfaces.AppViewModelListenerInterface
import us.huseli.soundboard_kotlin.interfaces.EditCategoryInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel

class CategoryAdapter(private val fragment: Fragment, private val appViewModel: AppViewModel) :
        DataBoundListAdapter<CategoryViewModel, CategoryAdapter.ViewHolder, ItemCategoryBinding>(Companion) {
    private val soundViewPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 20) }

    companion object : DiffUtil.ItemCallback<CategoryViewModel>() {
        override fun areItemsTheSame(oldItem: CategoryViewModel, newItem: CategoryViewModel) = oldItem === newItem
        override fun areContentsTheSame(oldItem: CategoryViewModel, newItem: CategoryViewModel) = oldItem.id == newItem.id
    }

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)

    override fun createViewHolder(binding: ItemCategoryBinding, parent: ViewGroup) = ViewHolder(binding)

    override fun bind(holder: ViewHolder, item: CategoryViewModel) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.bind(item)
    }

    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    inner class ViewHolder(binding: ItemCategoryBinding) :
            DataBoundViewHolder<ItemCategoryBinding>(binding),
            View.OnClickListener,
            AppViewModelListenerInterface {
        private lateinit var categoryViewModel: CategoryViewModel
        private lateinit var soundAdapter: SoundAdapter
        private lateinit var itemTouchHelper: ItemTouchHelper

        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)
            binding.categoryMoveButton
        }

        fun bind(categoryViewModel: CategoryViewModel) {
            Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter.ViewHolder ${hashCode()} bind CategoryViewModel ${categoryViewModel.hashCode()}")
            this.categoryViewModel = categoryViewModel
            binding.categoryViewModel = categoryViewModel

            // Create "sub-adapter" SoundAdapter and do various bindings
            soundAdapter = SoundAdapter(fragment, appViewModel, categoryViewModel)
            itemTouchHelper = ItemTouchHelper(SoundItemTouchHelperCallback(soundAdapter))
            binding.soundList.apply {
                adapter = soundAdapter
                layoutManager = GridLayoutManager(context, zoomLevelToSpanCount(0))
                setRecycledViewPool(soundViewPool)
            }
            categoryViewModel.soundListViewModel.soundViewModels.observe(this, Observer { soundAdapter.submitList(it) })

            // Observe changes in zoomLevel and reorderEnabled
            appViewModel.zoomLevel.observe(this, Observer { value -> onZoomLevelChange(value) })
            appViewModel.reorderEnabled.observe(this, Observer { value -> onReorderEnabledChange(value) })
        }

        override fun onClick(v: View?) {
            // When icons in the category header are clicked
            val activity = binding.root.context as EditCategoryInterface
            val categoryId = categoryViewModel.id!!
            when (v) {
                binding.categoryEditButton -> activity.showCategoryEditDialog(categoryId)
                binding.categoryDeleteButton -> activity.showCategoryDeleteDialog(categoryId)
            }
        }

        override fun onReorderEnabledChange(value: Boolean) =
                if (value) itemTouchHelper.attachToRecyclerView(binding.soundList) else itemTouchHelper.attachToRecyclerView(null)

        override fun onZoomLevelChange(value: Int) {
            Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter.ViewHolder ${hashCode()} onZoomLevelChange: $value")
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
    }
}