package us.huseli.soundboard_kotlin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter
import us.huseli.soundboard_kotlin.databinding.FragmentCategoryListBinding
import us.huseli.soundboard_kotlin.helpers.CategoryItemDragHelperCallback
import us.huseli.soundboard_kotlin.interfaces.StartDragListenerInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel

class CategoryListFragment : Fragment(), StartDragListenerInterface {
    private val appViewModel by activityViewModels<AppViewModel>()
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()

    private lateinit var binding: FragmentCategoryListBinding
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onCreateView")
        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        val adapter = CategoryAdapter(this, categoryListViewModel, appViewModel)

        itemTouchHelper = ItemTouchHelper(CategoryItemDragHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.categoryList)

        binding.categoryList.adapter = adapter
        binding.categoryList.layoutManager = LinearLayoutManager(requireContext())

        categoryListViewModel.categories.observe(viewLifecycleOwner, {
            Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment: categoryListViewModel.categoryViewModels changed: $it")
            adapter.submitList(it.toMutableList())
        })
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)
}