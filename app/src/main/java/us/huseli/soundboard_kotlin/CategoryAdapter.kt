package us.huseli.soundboard_kotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_category.view.*
import us.huseli.soundboard_kotlin.data.Category
import us.huseli.soundboard_kotlin.data.CategoryListViewModel
import us.huseli.soundboard_kotlin.data.CategoryViewModel
import us.huseli.soundboard_kotlin.databinding.ItemCategoryBinding

class CategoryAdapter(
        private val viewModel: CategoryListViewModel,
        private val viewLifecycleOwner: LifecycleOwner,
        private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    private var categories: MutableList<Category> = emptyList<Category>().toMutableList()

    class CategoryViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        internal lateinit var viewModel: CategoryViewModel
        internal lateinit var viewLifecycleOwner: LifecycleOwner
        internal lateinit var fragmentManager: FragmentManager
        //private val soundListView: RecyclerView = view.sound_list_container.sound_list
        // private val nameTextView: TextView = view.category_name

        private fun columnCountAtZoomLevelZero(): Int {
            return binding.root.resources.configuration.screenWidthDp / 80
        }

        fun bind(viewModel: CategoryViewModel) {
            this.viewModel = viewModel
            // nameTextView.text = viewModel.category.name
            binding.viewModel = viewModel
            // binding.executePendingBindings()
            fragmentManager.beginTransaction().apply {
                add(binding.root.sound_list_container.id, SoundListFragment.newInstance(viewModel.category.id!!))
                commit()
            }
/*
        soundListView.layoutManager = GridLayoutManager(view.context, columnCountAtZoomLevelZero())
        soundListView.adapter = SoundAdapter().apply {
            viewModel.sounds.observe(viewLifecycleOwner, Observer { setSounds(it) })
        }
*/
        }
    }

    internal fun setCategories(categories: List<Category>) {
        this.categories = categories as MutableList<Category>
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // view = ConstraintLayout from item_category
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCategoryBinding.inflate(inflater)
        return CategoryViewHolder(binding).also {
            it.viewLifecycleOwner = viewLifecycleOwner
            it.fragmentManager = fragmentManager
        }
    }

    override fun getItemCount(): Int = viewModel.categories.value?.size ?: 0

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        viewModel.categories.value?.get(position)?.let {
            holder.viewModel = CategoryViewModel.getInstance(GlobalApplication.application, it)
        }
    }
}