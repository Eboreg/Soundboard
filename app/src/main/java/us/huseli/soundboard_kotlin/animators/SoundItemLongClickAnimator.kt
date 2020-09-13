package us.huseli.soundboard_kotlin.animators

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import androidx.cardview.widget.CardView

class SoundItemLongClickAnimator(view: CardView, private val originalColor: Int) {
    private val animateIn: ObjectAnimator?
    private val animateOut: ObjectAnimator?

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            animateIn = ObjectAnimator.ofArgb(view, "cardBackgroundColor", originalColor, Color.WHITE).apply { duration = 100 }
            animateOut = ObjectAnimator.ofArgb(view, "cardBackgroundColor", Color.WHITE, originalColor).apply { duration = 300 }
        } else {
            animateIn = null
            animateOut = null
        }
    }

    fun start() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AnimatorSet().apply {
                play(animateIn).before(animateOut)
                start()
            }
        }
    }
}