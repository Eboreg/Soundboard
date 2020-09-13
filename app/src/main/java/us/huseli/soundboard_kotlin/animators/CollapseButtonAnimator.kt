package us.huseli.soundboard_kotlin.animators

import android.animation.ObjectAnimator
import android.view.View

class CollapseButtonAnimator(target: View) {
    private val duration: Long = 300
    private val collapseAnim = ObjectAnimator.ofFloat(target, "rotation", 0f, -90f).apply {
        duration = this@CollapseButtonAnimator.duration
    }
    private val uncollapseAnim = ObjectAnimator.ofFloat(target, "rotation", -90f, 0f).apply {
        duration = this@CollapseButtonAnimator.duration
    }

    fun animate(collapsed: Boolean) {
        if (collapsed)
            collapseAnim.start()
        else
            uncollapseAnim.start()
    }
}