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
import us.huseli.soundboard_kotlin.helpers.CategoryItemTouchHelperCallback
import us.huseli.soundboard_kotlin.interfaces.StartDragListenerInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel

class CategoryListFragment : Fragment(), StartDragListenerInterface {
    private val appViewModel by activityViewModels<AppViewModel>()
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()

    private lateinit var binding: FragmentCategoryListBinding
    private lateinit var itemTouchHelper: ItemTouchHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onCreateView")
        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        val adapter = CategoryAdapter(this, appViewModel).apply { registerAdapterDataObserver(AdapterDataObserver()) }
        itemTouchHelper = ItemTouchHelper(CategoryItemTouchHelperCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.categoryList)
        binding.categoryList.adapter = adapter
        binding.categoryList.layoutManager = LinearLayoutManager(requireContext())

        categoryListViewModel.categoryViewModels.observe(viewLifecycleOwner, { adapter.submitList(it) })
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) = itemTouchHelper.startDrag(viewHolder)


    inner class AdapterDataObserver : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            categoryListViewModel.updateOrder(fromPosition, toPosition)
        }
    }
}