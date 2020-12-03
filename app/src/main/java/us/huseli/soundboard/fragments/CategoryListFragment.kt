package us.huseli.soundboard.fragments

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
import us.huseli.soundboard.GlobalApplication
import us.huseli.soundboard.adapters.CategoryAdapter
import us.huseli.soundboard.databinding.FragmentCategoryListBinding
import us.huseli.soundboard.interfaces.ZoomInterface
import us.huseli.soundboard.viewmodels.AppViewModel
import us.huseli.soundboard.viewmodels.CategoryListViewModel
import us.huseli.soundboard.viewmodels.SoundViewModel

class CategoryListFragment : Fragment(), View.OnTouchListener {
    private val categoryListViewModel by activityViewModels<CategoryListViewModel>()
    private val appViewModel by activityViewModels<AppViewModel>()
    private val soundViewModel by activityViewModels<SoundViewModel>()
    private val preferences: SharedPreferences by lazy { requireActivity().getPreferences(Context.MODE_PRIVATE) }
    private val scaleGestureDetector by lazy { ScaleGestureDetector(requireContext(), ScaleListener()) }

    private var categoryAdapter: CategoryAdapter? = null
    private var binding: FragmentCategoryListBinding? = null
    private var initialSpanCount: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val config = resources.configuration

        val landscapeSpanCount = preferences.getInt("landscapeSpanCount", 0)
        initialSpanCount = appViewModel.setup(config.orientation, config.screenWidthDp, config.screenHeightDp, landscapeSpanCount)
        appViewModel.spanCountLandscape.observe(viewLifecycleOwner) { preferences.edit {
            putInt("landscapeSpanCount", it)
            apply()
        }}

        binding = FragmentCategoryListBinding.inflate(inflater, container, false)
        return binding?.let { binding ->
            binding.lifecycleOwner = viewLifecycleOwner
            binding.root
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryAdapter = CategoryAdapter(
                appViewModel,
                initialSpanCount ?: AppViewModel.DEFAULT_SPANCOUNT_PORTRAIT,
                soundViewModel,
                categoryListViewModel,
                requireActivity()
        ).also { categoryAdapter ->
            binding?.also { binding ->
                binding.categoryList.apply {
                    categoryAdapter.itemTouchHelper.attachToRecyclerView(this)
                    adapter = categoryAdapter
                    layoutManager = LinearLayoutManager(requireContext()).apply {
                        isItemPrefetchEnabled = true
                    }
                    setOnTouchListener(this@CategoryListFragment)
                }

                categoryListViewModel.categories.observe(viewLifecycleOwner) {
                    Log.i(GlobalApplication.LOG_TAG,
                            "CategoryListFragment: categoryListViewModel.categories changed: $it, " +
                                    "recyclerView ${binding.categoryList.hashCode()}, " +
                                    "sending to CategoryAdapter ${categoryAdapter.hashCode()}")
                    // Cache 'em all - this shit needs to be fast
                    binding.categoryList.setItemViewCacheSize(it.size)
                    categoryAdapter.submitList(it)
                }
            } ?: run {
                Log.e(LOG_TAG, "onViewCreated: binding is null")
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        categoryAdapter?.setLifecycleDestroyed()
        categoryAdapter = null
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


    companion object {
        const val LOG_TAG = "CategoryListFragment"
    }
}