package us.huseli.soundboard_kotlin

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import us.huseli.soundboard_kotlin.data.SoundViewModel

class SoundListFragment : Fragment() {
    private var mColumnCount = 4
    private val viewModel: SoundViewModel by activityViewModels()
    private lateinit var adapter: SoundAdapter

    companion object {
        private const val ARG_COLUMN_COUNT = "column-count"
        const val ARG_SOUND_ID = "soundId"
        const val ARG_SOUND_NAME = "soundName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mColumnCount = requireArguments().getInt(ARG_COLUMN_COUNT)
        }
        adapter = SoundAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_sound_list, container, false) as RecyclerView
        view.adapter = adapter

        val helper = ItemTouchHelper(SoundItemTouchHelperCallback(adapter))
        helper.attachToRecyclerView(view)


        if (mColumnCount <= 1) {
            view.layoutManager = LinearLayoutManager(view.context)
        } else {
            view.layoutManager = GridLayoutManager(view.context, mColumnCount)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerForContextMenu(view)

        viewModel.sounds.observe(
                viewLifecycleOwner,
                Observer { sounds ->
                    sounds?.let { adapter.setSounds(it) }
                }
        )
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        activity?.menuInflater?.inflate(R.menu.sound_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val sound = adapter.currentSound!!
        when (item.itemId) {
            R.id.sound_context_menu_edit -> (activity as EditSoundInterface).showEditDialog(sound)
            R.id.sound_context_menu_delete -> {
                sound.id?.let { viewModel.deleteSound(it) }
            }
        }
        return true
    }
}