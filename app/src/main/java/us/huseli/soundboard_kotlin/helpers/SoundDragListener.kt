package us.huseli.soundboard_kotlin.helpers

import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.sentry.core.Sentry
import us.huseli.soundboard_kotlin.adapters.SoundAdapter

class SoundDragListener : View.OnDragListener {
    private var isDropped = false
    private var targetRecyclerView: RecyclerView? = null

    fun onDrop(source: SoundAdapter.ViewHolder, target: SoundAdapter.ViewHolder) {
        source.adapter.currentList.remove(source.sound)
        target.adapter.currentList.add(target.adapterPosition, source.sound!!)
        // TODO: Update DB in both (notifySomething())
    }

    override fun onDrag(targetView: View, event: DragEvent): Boolean {
        // v = target RecyclerView
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                isDropped = true
                try {
                    val sourceView = event.localState as View
                    val sourceList = sourceView.parent as RecyclerView
                    val sourcePosition = sourceView.tag as Int
                    val sourceAdapter = sourceList.adapter as SoundAdapter
                    val sourceViewHolder = sourceAdapter.getViewHolder(sourcePosition)

                    val targetList = targetView.parent as RecyclerView
                    val targetPosition = targetView.tag as Int
                    val targetAdapter = targetList.adapter as SoundAdapter
                    val targetViewHolder = targetAdapter.getViewHolder(targetPosition)

                    onDrop(sourceViewHolder, targetViewHolder)
                } catch (e: TypeCastException) {
                    Sentry.captureException(e)
                }
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
            }
        }
        return true
    }
}