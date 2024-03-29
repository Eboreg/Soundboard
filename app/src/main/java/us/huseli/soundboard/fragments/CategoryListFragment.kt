package us.huseli.soundboard.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.soundboard.adapters.CategoryAdapter
import us.huseli.soundboard.databinding.FragmentCategoryListBinding
import us.huseli.soundboard.helpers.SoundScroller
import us.huseli.soundboard.interfaces.ZoomInterface
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.CategoryViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel

@AndroidEntryPoint
class CategoryListFragment : Fragment(), View.OnTouchListener {
    private val categoryListViewModel by activityViewModels<CategoryViewModel>()
    private val appViewModel by activityViewModels<AppViewModel>()
    private val soundViewModel by activityViewModels<SoundViewModel>()
    private val scaleGestureDetector by lazy { ScaleGestureDetector(requireContext(), ScaleListener()) }

    private lateinit var binding: FragmentCategoryListBinding

    private var categoryAdapter: CategoryAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // binding = null
        categoryAdapter?.setLifecycleDestroyed()
        categoryAdapter = null
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryAdapter = CategoryAdapter(
            appViewModel,
            soundViewModel,
            categoryListViewModel,
            requireActivity(),
            SoundScroller(binding.categoryList, 10, 10)
        ).also { categoryAdapter ->
            binding.categoryList.apply {
                adapter = categoryAdapter
                layoutManager?.isItemPrefetchEnabled = true
                setHasFixedSize(true)
                setOnTouchListener(this@CategoryListFragment)
            }

            categoryListViewModel.categories.observe(viewLifecycleOwner) {
                // Cache 'em all - this shit needs to be fast
                binding.categoryList.setItemViewCacheSize(it.size)
                categoryAdapter.submitList(it)
            }
        }
    }


    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.scaleFactor?.let { scaleFactor ->
                if (scaleFactor <= 0.8) {
                    (requireActivity() as ZoomInterface).zoomOut()
                    return true
                } else if (scaleFactor >= 1.3) {
                    (requireActivity() as ZoomInterface).zoomIn()
                    return true
                }
            }
            return super.onScale(detector)
        }
    }


    companion object {
        const val LOG_TAG = "CategoryListFragment"
    }
}