package us.huseli.soundboard_kotlin

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_sound_list.view.*
import us.huseli.soundboard_kotlin.data.SoundListViewModel
import us.huseli.soundboard_kotlin.helpers.EditSoundInterface
import us.huseli.soundboard_kotlin.helpers.SoundItemTouchHelperCallback

class SoundListFragment : Fragment() {
    private val listViewModel: SoundListViewModel by activityViewModels()
    private lateinit var adapter: SoundAdapter
    // private lateinit var view: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val categoryId: Int? by lazy { arguments?.getInt(ARG_CATEGORY_ID, 0) }

    private fun columnCountAtZoomLevelZero(): Int {
        return resources.configuration.screenWidthDp / 80
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_sound_list, container, false)
        // TODO: move to onViewCreated?
        adapter = SoundAdapter()
        recyclerView = view.sound_list
        recyclerView.adapter = adapter

        // To keep us informed of items changing places
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                listViewModel.updateSoundOrder(fromPosition, toPosition)
            }
        })

        itemTouchHelper = ItemTouchHelper(SoundItemTouchHelperCallback(adapter))
        recyclerView.layoutManager = GridLayoutManager(view.context, columnCountAtZoomLevelZero())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerForContextMenu(view)

        listViewModel.categoryId = categoryId
        listViewModel.sounds.observe(viewLifecycleOwner, Observer { sounds -> sounds?.let { adapter.setSounds(it) }})
        listViewModel.reorderEnabled.observe(viewLifecycleOwner, Observer { onReorderEnabledChange(it) })
        listViewModel.zoomLevel.observe(viewLifecycleOwner, Observer { onZoomLevelChange(it, view as RecyclerView) })
    }

    private fun onReorderEnabledChange(value: Boolean) {
        if (value) {
            unregisterForContextMenu(recyclerView)
            itemTouchHelper.attachToRecyclerView(recyclerView)
        } else {
            itemTouchHelper.attachToRecyclerView(null)
            registerForContextMenu(recyclerView)
        }
    }

    private fun onZoomLevelChange(value: Int?, view: RecyclerView) {
        // Default = zoomLevel 0
        // zoomLevel 1 = minus one spanCount etc
        if (value != null) (view.layoutManager as GridLayoutManager).apply {
            val newSpanCount = columnCountAtZoomLevelZero() - value
            if (newSpanCount > 0)
                spanCount = newSpanCount
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        activity?.menuInflater?.inflate(R.menu.sound_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        //val soundId = adapter.currentSoundId!!
        val soundViewModel = adapter.currentSoundViewModel!!
        when (item.itemId) {
            R.id.sound_context_menu_edit -> (activity as EditSoundInterface).showSoundEditDialog(soundViewModel)
            R.id.sound_context_menu_delete -> soundViewModel.sound.id?.let { listViewModel.deleteSound(it) }
        }
        return true
    }

    companion object {
        const val ARG_CATEGORY_ID = "categoryId"

        fun newInstance(categoryId: Int): SoundListFragment {
            return SoundListFragment().apply {
                arguments = Bundle().also { it.putInt(ARG_CATEGORY_ID, categoryId) }
            }
        }
    }
}