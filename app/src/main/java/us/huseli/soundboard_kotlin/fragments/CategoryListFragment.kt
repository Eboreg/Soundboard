package us.huseli.soundboard_kotlin.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.adapters.CategoryAdapter
import us.huseli.soundboard_kotlin.databinding.FragmentCategoryListBinding
import us.huseli.soundboard_kotlin.interfaces.ZoomInterface
import us.huseli.soundboard_kotlin.viewmodels.AppViewModel
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel

class CategoryListFragment : Fragment(), View.OnTouchListener {
    val categoryListViewModel by activityViewModels<CategoryListViewModel>()

    private val appViewModel by activityViewModels<AppViewModel>()
    private val preferences: SharedPreferences by lazy { requireActivity().getPreferences(Context.MODE_PRIVATE) }
    private val scaleGestureDetector by lazy { ScaleGestureDetector(requireContext(), ScaleListener()) }

    private lateinit var binding: FragmentCategoryListBinding
    private var initialSpanCount: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = resources.configuration

        val landscapeSpanCount = preferences.getInt("landscapeSpanCount", 0)
        initialSpanCount = appViewModel.setup(config.orientation, config.screenWidthDp, config.screenHeightDp, landscapeSpanCount)
        appViewModel.spanCountLandscape.observe(viewLifecycleOwner, { preferences.edit {
            putInt("landscapeSpanCount", it)
            apply()
        }})

        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryAdapter = CategoryAdapter(requireActivity(), categoryListViewModel, appViewModel, initialSpanCount!!)

        binding.categoryList.apply {
            categoryAdapter.itemTouchHelper.attachToRecyclerView(this)
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.categoryList.setOnTouchListener(this)

        categoryListViewModel.categories.observe(viewLifecycleOwner, {
            Log.i(GlobalApplication.LOG_TAG,
                    "CategoryListFragment: categoryListViewModel.categories changed: $it, " +
                            "recyclerView ${binding.categoryList.hashCode()}, " +
                            "sending to CategoryAdapter ${categoryAdapter.hashCode()}")
            categoryAdapter.submitList(it)
        })
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        // Seems like we have to do this here and not in MainActivity, because otherwise
        // RecyclerView consumes the touch events and they never reach MainActivity.onTouch()
        when (event?.actionMasked) {
            MotionEvent.ACTION_UP -> {
                view?.performClick()
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) return false
            }
        }
        return scaleGestureDetector.onTouchEvent(event)
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.scaleFactor?.let { scaleFactor ->
                if (scaleFactor <= 0.7) {
                    (requireActivity() as ZoomInterface).zoomOut()
                    return true
                } else if (scaleFactor >= 1.4) {
                    (requireActivity() as ZoomInterface).zoomIn()
                    return true
                }
            }
            return super.onScale(detector)
        }
    }
}