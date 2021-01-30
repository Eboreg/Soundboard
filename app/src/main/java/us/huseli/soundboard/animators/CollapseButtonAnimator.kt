package us.huseli.soundboard.animators

import android.animation.ObjectAnimator
import android.view.View

class CollapseButtonAnimator(target: View) {
    private val collapseAnim = ObjectAnimator.ofFloat(target, "rotation", 0f, -90f)
    private val expandAnim = ObjectAnimator.ofFloat(target, "rotation", -90f, 0f)

    fun animate(collapsed: Boolean) {
        if (collapsed) collapseAnim.start()
        else expandAnim.start()
    }
}