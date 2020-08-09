package us.huseli.soundboard_kotlin.adapters.common

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentContainerView
import us.huseli.soundboard_kotlin.GlobalApplication
import us.huseli.soundboard_kotlin.data.Category

@BindingAdapter("drawableColor")
//fun setDrawableColor(view: ImageView, color: Int) = (view.drawable as GradientDrawable).setColor(color)
fun setDrawableColor(view: ImageView, color: Int) {
    when(view.drawable) {
        is GradientDrawable -> (view.drawable as GradientDrawable).setColor(color)
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (view.drawable as Drawable).setTint(color)
            } else {
                TODO("VERSION.SDK_INT < LOLLIPOP")
            }
        }
    }
}

/*
fun FragmentContainerView.setCategory(category: Category?) {
}
*/
