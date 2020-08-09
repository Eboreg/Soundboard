package us.huseli.soundboard_kotlin.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.MainActivity
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.common.DataBoundListAdapter
import us.huseli.soundboard_kotlin.adapters.common.DataBoundViewHolder
import us.huseli.soundboard_kotlin.databinding.ItemCategoryBinding
import us.huseli.soundboard_kotlin.fragments.CategoryListFragment
import us.huseli.soundboard_kotlin.fragments.SoundListFragment
import us.huseli.soundboard_kotlin.viewmodels.CategoryViewModel

//class CategoryAdapter(private val fragment: CategoryListFragment) : ListAdapter<CategoryViewModel, CategoryAdapter.ViewHolder>(Companion) {
class CategoryAdapter(private val fragment: CategoryListFragment) : DataBoundListAdapter<CategoryViewModel, CategoryAdapter.ViewHolder, ItemCategoryBinding>(Companion) {
    private val viewPool = RecyclerView.RecycledViewPool()

    companion object : DiffUtil.ItemCallback<CategoryViewModel>() {
        override fun areItemsTheSame(oldItem: CategoryViewModel, newItem: CategoryViewModel) = oldItem === newItem
        override fun areContentsTheSame(oldItem: CategoryViewModel, newItem: CategoryViewModel) = oldItem.id.value == newItem.id.value
    }

    // TODO: Maybe run notifyItemChanged() when items changed?

    override fun createBinding(parent: ViewGroup, viewType: Int) =
            ItemCategoryBinding.inflate(LayoutInflater.from(parent.context))

    override fun createViewHolder(binding: ItemCategoryBinding, parent: ViewGroup) = ViewHolder(binding)

    override fun bind(holder: ViewHolder, item: CategoryViewModel) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter ${this.hashCode()}, bind holder ${holder.hashCode()} with viewmodel ${item.hashCode()}")
        holder.binding.categoryViewModel = item
        // holder.binding.soundListContainer.id = View.generateViewId()
        item.id.value?.let { ensureFragment(holder, it) }
    }

    private fun ensureFragment(holder: ViewHolder, categoryId: Int) {
        // On resume, like after a screen orientation flip, the SoundListFragments have already
        // been re-created here, and the old ones have been destroyed. But we have to attach them
        // to the sound_list_container FragmentContainerView maybe?
        holder.binding.soundListContainer.id = R.id.sound_list_container + categoryId
        val soundListFragment = fragment.childFragmentManager.findFragmentByTag("category$categoryId")
        if (soundListFragment != null)
            Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter ${this.hashCode()} ensureFragment: fragment ${soundListFragment.hashCode()} found")
        else {
            Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter ${this.hashCode()} ensureFragment: fragment NOT found")
            fragment.childFragmentManager.beginTransaction().apply {
                add(holder.binding.soundListContainer.id,
                        SoundListFragment.newInstance(categoryId).also { it.viewPool = viewPool },
                        "category$categoryId")
                commit()
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryAdapter ${this.hashCode()} onDetachedFromRecyclerView ${recyclerView.hashCode()}")
        super.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * Represents one individual category with its sound list.
     * Layout: item_category.xml, see this file for binding
     */
    inner class ViewHolder(binding: ItemCategoryBinding) : DataBoundViewHolder<ItemCategoryBinding>(binding), View.OnClickListener {
        init {
            binding.categoryEditButton.setOnClickListener(this)
            binding.categoryDeleteButton.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val activity = binding.root.context as MainActivity
            val categoryId = binding.categoryViewModel!!.id.value!!
            when (v) {
                binding.categoryEditButton -> {
                    activity.showCategoryEditDialog(categoryId)
                }
                binding.categoryDeleteButton -> {
                    activity.showCategoryDeleteDialog(categoryId)
                }
            }
        }
    }

}