package us.huseli.soundboard_kotlin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.fragment_sound.view.*
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundViewModel
import us.huseli.soundboard_kotlin.helpers.ItemTouchHelperAdapter

class SoundViewHolder(view: View, private val context: Context) : ViewHolder(view), View.OnClickListener, View.OnLongClickListener {
    private lateinit var adapter: SoundAdapter
    private lateinit var viewModel: SoundViewModel

    private val nameTextView: TextView
    private val playIcon: ImageView

    init {
        view.setOnClickListener(this)
        view.setOnLongClickListener(this)
        nameTextView = view.sound_name
        playIcon = view.play_icon
    }

    fun bind(viewModel: SoundViewModel, adapter: SoundAdapter) {
        this.viewModel = viewModel
        this.viewModel.setOnCompletionListener { pause() }
        this.adapter = adapter
        this.nameTextView.text = viewModel.name
    }

    override fun onLongClick(v: View?): Boolean {
        adapter.currentSoundViewModel = viewModel
        v?.showContextMenu()
        return true
    }

    override fun onClick(v: View?) {
        if (!viewModel.isValid) showErrorToast() else if (viewModel.isPlaying) pause() else play()
    }

    override fun toString(): String {
        return super.toString() + " '" + nameTextView.text + "'"
    }

    private fun play() {
        viewModel.play()
        playIcon.visibility = View.VISIBLE
    }

    private fun pause() {
        viewModel.pause()
        playIcon.visibility = View.INVISIBLE
    }

    private fun showErrorToast() {
        Toast.makeText(context, viewModel.errorMessage, Toast.LENGTH_SHORT).show()
    }
}


class SoundAdapter : RecyclerView.Adapter<SoundViewHolder>(), ItemTouchHelperAdapter {
    var currentSoundViewModel: SoundViewModel? = null

    private var sounds: MutableList<Sound> = emptyList<Sound>().toMutableList()
    private lateinit var context: Context

    // Called by SoundListFragment.onViewCreated
    internal fun setSounds(sounds: List<Sound>) {
        this.sounds = sounds as MutableList<Sound>
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        this.context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_sound, parent, false)
        return SoundViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(SoundViewModel.getInstance(context, sounds[position]), this)
    }

    override fun getItemCount(): Int = sounds.size

    override fun onItemMove(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    override fun onItemDismiss(position: Int) = Unit
}