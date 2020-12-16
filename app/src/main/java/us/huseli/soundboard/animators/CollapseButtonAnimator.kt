package us.huseli.soundboard.animators

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnEnd

class CollapseButtonAnimator(target: View, private val listView: View) {
    private val duration: Long = 300
    private val listCollapseAnim = ObjectAnimator.ofInt(listView, "visibility", View.VISIBLE, View.GONE).apply {
        duration = this@CollapseButtonAnimator.duration
        doOnEnd { listView.invalidate() }
    }
    private val iconCollapseAnim = ObjectAnimator.ofFloat(target, "rotation", 0f, -90f).apply {
        duration = this@CollapseButtonAnimator.duration
    }
    private val collapseAmin = AnimatorSet().apply {
        play(listCollapseAnim).with(iconCollapseAnim)
    }
    private val listUncollapseAmin = ObjectAnimator.ofInt(listView, "visibility", View.GONE, View.VISIBLE).apply {
        duration = this@CollapseButtonAnimator.duration
    }
    private val iconUncollapseAnim = ObjectAnimator.ofFloat(target, "rotation", -90f, 0f).apply {
        duration = this@CollapseButtonAnimator.duration
    }
    private val uncollapseAnim = AnimatorSet().apply {
        play(listUncollapseAmin).with(iconUncollapseAnim)
    }

    fun animate(collapsed: Boolean) {
        if (collapsed) iconCollapseAnim.start()
        else iconUncollapseAnim.start()
//        if (collapsed) collapseAmin.start()
//        else uncollapseAnim.start()
    }
}