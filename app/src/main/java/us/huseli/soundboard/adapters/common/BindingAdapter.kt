package us.huseli.soundboard.adapters.common

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.VectorDrawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("drawableColor")
fun setDrawableColor(view: ImageView, color: Int?) {
    if (color != null) {
        when (view.drawable) {
            is GradientDrawable -> (view.drawable as GradientDrawable).setColor(color)
            is VectorDrawable -> (view.drawable as VectorDrawable).apply {
                setTint(color)
                setTintMode(PorterDuff.Mode.SRC_IN)
            }
            else -> (view.drawable as Drawable).colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }
}