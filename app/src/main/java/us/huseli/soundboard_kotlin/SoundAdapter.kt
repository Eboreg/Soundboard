package us.huseli.soundboard_kotlin

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import us.huseli.soundboard_kotlin.data.Sound
import java.util.*

class SoundViewHolder(view: View, context: Context) : ViewHolder(view), View.OnClickListener, View.OnLongClickListener {
    private lateinit var adapter: SoundAdapter
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var sound: Sound

    private val context: Context
    private var errorMessage = ""
    private var isValid = true
    private val nameTextView: TextView
    private val playIcon: ImageView
    private val view: View

    init {
        view.setOnClickListener(this)
        view.setOnLongClickListener(this)
        this.view = view
        this.context = context
        nameTextView = view.findViewById<View>(R.id.sound_name) as TextView
        playIcon = itemView.findViewById<View>(R.id.play_icon) as ImageView
    }

    fun bind(sound: Sound, adapter: SoundAdapter) {
        this.sound = sound
        this.adapter = adapter
        this.nameTextView.text = sound.name
        try {
            mediaPlayer = MediaPlayer().also {
                it.setDataSource(context, sound.uri)
                it.prepare()
                it.setOnCompletionListener { pause() }
            }
        } catch (e: Exception) {
            isValid = false
            errorMessage = if (e.cause != null) e.cause.toString() else e.toString()
        }
    }

    override fun onLongClick(v: View?): Boolean {
        adapter.currentSound = sound
        v?.showContextMenu()
        return true
    }

    override fun onClick(v: View?) {
        if (!isValid) showErrorToast() else if (mediaPlayer.isPlaying) pause() else play()
    }

    override fun toString(): String {
        return super.toString() + " '" + nameTextView.text + "'"
    }

    private fun play() {
        mediaPlayer.start()
        playIcon.visibility = View.VISIBLE
    }

    private fun pause() {
        mediaPlayer.apply {
            pause()
            seekTo(0)
        }
        playIcon.visibility = View.INVISIBLE
    }

    private fun showErrorToast() {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}


class SoundAdapter : RecyclerView.Adapter<SoundViewHolder>(), ItemTouchHelperAdapter {
    var currentSound: Sound? = null

    private var sounds: MutableList<Sound> = emptyList<Sound>().toMutableList()
    private lateinit var context: Context

    internal fun setSounds(sounds: List<Sound>) {
        this.sounds = sounds as MutableList<Sound>
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        this.context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_sound, parent, false)
        return SoundViewHolder(view, context)
    }

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) = holder.bind(sounds[position], this)

    override fun getItemCount(): Int = sounds.size

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(sounds, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(sounds, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        // TODO: Används denna?
        sounds.removeAt(position)
        notifyItemRemoved(position)
    }
}