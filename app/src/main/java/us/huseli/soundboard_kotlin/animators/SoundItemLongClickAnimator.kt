package us.huseli.soundboard_kotlin.animators

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import androidx.cardview.widget.CardView

class SoundItemLongClickAnimator(view: CardView) {
    private val originalColor = view.cardBackgroundColor.defaultColor
    private val animateIn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        ObjectAnimator.ofArgb(view, "cardBackgroundColor", originalColor, Color.WHITE).apply { duration = 100 }
    } else null
    private val animateOut = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        ObjectAnimator.ofArgb(view, "cardBackgroundColor", Color.WHITE, originalColor).apply { duration = 300 }
    } else null

    fun start() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AnimatorSet().apply {
                play(animateIn).before(animateOut)
                //play(animateOut).after(animateIn)
                start()
            }
        }
    }
}