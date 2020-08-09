package us.huseli.soundboard_kotlin.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.R
import us.huseli.soundboard_kotlin.adapters.SoundAdapter
import us.huseli.soundboard_kotlin.databinding.FragmentSoundListBinding
import us.huseli.soundboard_kotlin.helpers.EditSoundInterface
import us.huseli.soundboard_kotlin.helpers.SoundItemTouchHelperCallback
import us.huseli.soundboard_kotlin.viewmodels.CategoryListViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModel
import us.huseli.soundboard_kotlin.viewmodels.SoundListViewModelFactory

class SoundListFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var adapter: SoundAdapter
    private lateinit var binding: FragmentSoundListBinding
    private lateinit var itemTouchHelper: ItemTouchHelper
    lateinit var viewPool: RecyclerView.RecycledViewPool
    private val categoryId: Int by lazy { requireArguments().getInt(ARG_CATEGORY_ID) }
    private val categoryListViewModel: CategoryListViewModel by activityViewModels()
    private val soundListViewModel: SoundListViewModel by viewModels { SoundListViewModelFactory(categoryId) }

    private fun columnCountAtZoomLevelZero(): Int {
        return resources.configuration.screenWidthDp / 80
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onCreateView")
        binding = FragmentSoundListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onActivityCreated")
        super.onActivityCreated(savedInstanceState)
        val preferences = requireActivity().getPreferences(Context.MODE_PRIVATE)
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        adapter = SoundAdapter(categoryListViewModel.get(categoryId)!!)
        binding.soundList.apply {
            adapter = this@SoundListFragment.adapter
            layoutManager = GridLayoutManager(view.context, columnCountAtZoomLevelZero())
        }

        itemTouchHelper = ItemTouchHelper(SoundItemTouchHelperCallback(adapter))
        registerForContextMenu(view)
    }

    override fun onStart() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onStart")
        super.onStart()
        soundListViewModel.soundViewModels.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })

        // To keep us informed of items changing places
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                soundListViewModel.updateSoundOrder(fromPosition, toPosition)
            }
        })
    }

    private fun onReorderEnabledChange(value: Boolean) {
        if (value) {
            unregisterForContextMenu(binding.soundList)
            itemTouchHelper.attachToRecyclerView(binding.soundList)
        } else {
            itemTouchHelper.attachToRecyclerView(null)
            registerForContextMenu(binding.soundList)
        }
    }

    private fun onZoomLevelChange(value: Int?) {
        // Default = zoomLevel 0
        // zoomLevel 1 = minus one spanCount etc
        if (value != null) (binding.soundList.layoutManager as GridLayoutManager).apply {
            val newSpanCount = columnCountAtZoomLevelZero() - value
            if (newSpanCount > 0)
                spanCount = newSpanCount
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences != null) {
            when(key) {
                "zoomLevel" -> { onZoomLevelChange(sharedPreferences.getInt("zoomLevel", 0)) }
                "reorderEnabled" -> { onReorderEnabledChange(sharedPreferences.getBoolean("reorderEnabled", false)) }
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.sound_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val soundId = adapter.currentSoundId!!
        when (item.itemId) {
            R.id.sound_context_menu_edit -> (requireActivity() as EditSoundInterface).showSoundEditDialog(soundId)
            R.id.sound_context_menu_delete -> soundListViewModel.deleteSound(soundId)
        }
        return true
    }

    override fun onDestroyView() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onDestroyView")
        super.onDestroyView()
        adapter.setLifecycleDestroyed()
    }

    companion object {
        const val ARG_CATEGORY_ID = "categoryId"

        @JvmStatic
        fun newInstance(categoryId: Int): SoundListFragment {
            Log.i(GlobalApplication.LOG_TAG, "SoundListFragment.newInstance, category $categoryId")
            return SoundListFragment().apply {
                arguments = Bundle().also { it.putInt(ARG_CATEGORY_ID, categoryId) }
            }
        }
    }

    override fun onAttach(context: Context) {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onAttach")
        super.onAttach(context)
    }

    override fun onPause() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onPause")
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onResume")
        super.onResume()
    }

    override fun onDetach() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onDetach")
        super.onDetach()
    }

    override fun onStop() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(GlobalApplication.LOG_TAG, "SoundListFragment ${this.hashCode()} onDestroy")
        super.onDestroy()
    }
}