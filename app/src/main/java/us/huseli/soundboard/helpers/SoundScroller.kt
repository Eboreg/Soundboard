package us.huseli.soundboard.helpers

import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.util.Log
import android.view.View

/**
 * @param view The view to do the scrolling (presumably the RecyclerView of categories)
 * @param distance The distance (in pixels) to scroll at a time
 * @param interval The number of milliseconds between the scrollings
 */
class SoundScroller(private val view: View, private val distance: Int, private val interval: Int) {
    private var direction: Direction? = null
    private val handler = Handler(this)
    private var isScrolling = false
    private var realDistance = distance
    private var verticalLimits = VerticalLimits(0, 0)

    fun scrollIfNecessary(draggedItemCenter: Float, draggedItemHeight: Int) {
        val rvPosition = IntArray(2)
        view.getLocationOnScreen(rvPosition)
        verticalLimits = VerticalLimits(rvPosition[1], rvPosition[1] + view.height)

        val scrollUpBreakpoint = draggedItemCenter - (draggedItemHeight / 2)
        val scrollUpFastBreakpoint = draggedItemCenter - (draggedItemHeight / 10)
        val scrollDownBreakpoint = draggedItemCenter + (draggedItemHeight / 2)
        val scrollDownFastBreakpoint = draggedItemCenter + (draggedItemHeight / 10)

        Log.d(LOG_TAG, "scrollIfNecessary: draggedItemCenter=$draggedItemCenter, draggedItemHeight=$draggedItemHeight, verticalLimits=$verticalLimits, scrollUpBreakpoint=$scrollUpBreakpoint, scrollUpFastBreakpoint=$scrollUpFastBreakpoint, scrollDownBreakpoint=$scrollDownBreakpoint, scrollDownFastBreakpoint=$scrollDownFastBreakpoint")

        when {
            scrollUpFastBreakpoint < verticalLimits.top -> start(Direction.UP, true)
            scrollUpBreakpoint < verticalLimits.top -> start(Direction.UP)
            scrollDownFastBreakpoint > verticalLimits.bottom -> start(Direction.DOWN, true)
            scrollDownBreakpoint > verticalLimits.bottom -> start(Direction.DOWN)
            else -> stop()
        }
    }

    private fun scroll() {
        Log.d(LOG_TAG, "scroll: realDistance=$realDistance, direction=$direction")
        when (direction) {
            Direction.DOWN -> view.scrollBy(0, realDistance)
            Direction.UP -> view.scrollBy(0, -realDistance)
        }
    }

    @Synchronized
    private fun start(direction: Direction, fast: Boolean) {
        realDistance = when (fast) {
            true -> distance * 3
            false -> distance
        }
        if (!isScrolling) {
            isScrolling = true
            this.direction = direction
            handler.sendMessage(handler.obtainMessage(MSG))
        }
    }

    @Synchronized
    private fun start(direction: Direction) = start(direction, false)

    @Synchronized
    private fun stop() {
        isScrolling = false
        handler.removeMessages(MSG)
    }


    enum class Direction { UP, DOWN }


    data class VerticalLimits(val top: Int, val bottom: Int)


    companion object {
        const val MSG = 1
        const val LOG_TAG = "SoundScroller"

        class Handler(private val soundScroller: SoundScroller) : android.os.Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                synchronized(soundScroller) {
                    if (soundScroller.isScrolling) {
                        val lastTickStart = SystemClock.elapsedRealtime()
                        soundScroller.scroll()
                        val lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart
                        val delay = soundScroller.interval - lastTickDuration
                        sendMessageDelayed(obtainMessage(MSG), delay)
                    }
                }
            }
        }
    }
}