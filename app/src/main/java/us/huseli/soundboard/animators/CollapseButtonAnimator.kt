package us.huseli.soundboard.animators

import android.animation.ObjectAnimator
import android.view.View

class CollapseButtonAnimator(target: View) {
    private val duration: Long = 300
    private val iconCollapseAnim = ObjectAnimator.ofFloat(target, "rotation", 0f, -90f).apply {
        duration = this@CollapseButtonAnimator.duration
    }
    private val iconUncollapseAnim = ObjectAnimator.ofFloat(target, "rotation", -90f, 0f).apply {
        duration = this@CollapseButtonAnimator.duration
    }

    fun animate(collapsed: Boolean) {
        if (collapsed) iconCollapseAnim.start()
        else iconUncollapseAnim.start()
    }
}