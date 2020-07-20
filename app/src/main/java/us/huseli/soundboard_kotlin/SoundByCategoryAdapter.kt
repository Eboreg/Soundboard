package us.huseli.soundboard_kotlin

import android.view.LayoutInflater
import android.view.ViewGroup
import us.huseli.soundboard_kotlin.data.Sound
import us.huseli.soundboard_kotlin.data.SoundViewModel
import us.huseli.soundboard_kotlin.helpers.ItemTouchHelperAdapter

class SoundByCategoryAdapter : SoundAdapter(), ItemTouchHelperAdapter {
    private var sounds: MutableList<Sound> = emptyList<Sound>().toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoundViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sound, parent, false)
        return SoundViewHolder(view)
    }

    override fun getItemCount() = sounds.size

    override fun onBindViewHolder(holder: SoundViewHolder, position: Int) {
        holder.bind(SoundViewModel.getInstance(GlobalApplication.application, sounds[position]), this)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) = notifyItemMoved(fromPosition, toPosition)

    override fun onItemDismiss(position: Int) {}
}