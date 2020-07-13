package us.huseli.soundboard_kotlin

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.data.SoundListViewModel

class SoundListFragment : Fragment() {
    private val listViewModel: SoundListViewModel by activityViewModels()
    private lateinit var adapter: SoundAdapter
    private lateinit var view: RecyclerView
    private lateinit var itemTouchHelper: ItemTouchHelper

    private fun columnCountAtZoomLevelZero(): Int {
        return resources.configuration.screenWidthDp / 80
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        adapter = SoundAdapter()
        view = inflater.inflate(R.layout.fragment_sound_list, container, false) as RecyclerView
        view.adapter = adapter

        // To keep us informed of items changing places
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                listViewModel.updateSoundOrder(fromPosition, toPosition)
            }
        })

        itemTouchHelper = ItemTouchHelper(SoundItemTouchHelperCallback(adapter))

        view.layoutManager = GridLayoutManager(view.context, columnCountAtZoomLevelZero())

        return view
    }

    private fun onReorderEnabledChange(value: Boolean) {
        if (value) {
            unregisterForContextMenu(view)
            itemTouchHelper.attachToRecyclerView(view)
        } else {
            itemTouchHelper.attachToRecyclerView(null)
            registerForContextMenu(view)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: move to onCreateView?
        registerForContextMenu(view)

        listViewModel.sounds.observe(viewLifecycleOwner, Observer { sounds -> sounds?.let { adapter.setSounds(it) }})
        listViewModel.reorderEnabled.observe(viewLifecycleOwner, Observer { onReorderEnabledChange(it) })
        listViewModel.zoomLevel.observe(viewLifecycleOwner, Observer { onZoomLevelChange(it, view as RecyclerView) })
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
            R.id.sound_context_menu_edit -> (activity as EditSoundInterface).showEditDialog(soundViewModel)
            R.id.sound_context_menu_delete -> soundViewModel.id?.let { listViewModel.deleteSound(it) }
        }
        return true
    }
}