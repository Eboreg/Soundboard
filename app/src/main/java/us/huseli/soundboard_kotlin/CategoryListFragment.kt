package us.huseli.soundboard_kotlin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_category_list.view.*
import us.huseli.soundboard_kotlin.data.CategoryListViewModel
import us.huseli.soundboard_kotlin.databinding.FragmentCategoryListBinding

class CategoryListFragment : Fragment() {
    private val viewModel: CategoryListViewModel by viewModels()
    private lateinit var binding: FragmentCategoryListBinding
    private lateinit var adapter: CategoryAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // It is recommended to only inflate the layout in this method and move logic that operates
        // on the returned View to onViewCreated(View, Bundle).
        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = requireActivity()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.category_list

        adapter = CategoryAdapter(viewModel, this, parentFragmentManager)
        recyclerView.adapter = adapter
        binding

        viewModel.categories.observe(viewLifecycleOwner, Observer { adapter.setCategories(it) })
    }
}