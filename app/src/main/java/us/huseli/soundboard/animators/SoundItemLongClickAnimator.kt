package us.huseli.soundboard.animators

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import androidx.cardview.widget.CardView
import us.huseli.soundboard.GlobalApplication

class SoundItemLongClickAnimator(view: CardView, originalColor: Int) {
    private val animateIn: ObjectAnimator?
    private val animateOut: ObjectAnimator?
    private val flashColor = if (GlobalApplication.application.getColorHelper().getLuminance(originalColor) >= 0.9) Color.BLACK else Color.WHITE

    init {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            animateIn = ObjectAnimator.ofArgb(view, "cardBackgroundColor", originalColor, flashColor).apply { duration = 100 }
            animateOut = ObjectAnimator.ofArgb(view, "cardBackgroundColor", flashColor, originalColor).apply { duration = 300 }
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