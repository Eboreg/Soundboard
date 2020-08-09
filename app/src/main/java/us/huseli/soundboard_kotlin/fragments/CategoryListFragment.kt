package us.huseli.soundboard_kotlin.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter
import us.huseli.soundboard_kotlin.databinding.FragmentCategoryListBinding
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel

class CategoryListFragment : Fragment() {
    private val categoryListViewModel: CategoryListViewModel by activityViewModels()
    private lateinit var binding: FragmentCategoryListBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onCreateView")
        // It is recommended to only inflate the layout in this method and move logic that operates
        // on the returned View to onViewCreated(View, Bundle).
        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        binding.categoryList.adapter = CategoryAdapter(this)

        categoryListViewModel.categoryViewModels.observe(viewLifecycleOwner, Observer {
            (binding.categoryList.adapter as CategoryAdapter).submitList(it)
        })
    }

    override fun onAttach(context: Context) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onAttach")
        super.onAttach(context)
    }

    override fun onPause() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onPause")
        super.onPause()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onActivityCreated")
        super.onActivityCreated(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onResume")
        super.onResume()
    }

    override fun onDetach() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onDetach")
        super.onDetach()
    }

    override fun onDestroyView() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onDestroyView")
        super.onDestroyView()
    }

    override fun onStop() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(GlobalApplication.LOG_TAG, "CategoryListFragment ${this.hashCode()} onDestroy")
        super.onDestroy()
    }
}