package us.huseli.soundboard_kotlin.helpers

import android.content.Context
import android.graphics.Color
import androidx.core.content.res.ResourcesCompat
import us.huseli.soundboard_kotlin.R

class ColorHelper(private val context: Context) {
    private val colorResources = arrayListOf(R.color.amber_500, R.color.black, R.color.blue_500, R.color.blue_grey_500,
            R.color.brown_500, R.color.cyan_500, R.color.deep_orange_500, R.color.deep_purple_500, R.color.green_500, R.color.grey_500,
            R.color.indigo_500, R.color.light_blue_500, R.color.light_green_500, R.color.lime_500, R.color.orange_500, R.color.pink_500,
            R.color.purple_500, R.color.red_500, R.color.teal_500, R.color.white, R.color.yellow_500)
    val colors = colorResources.map { ResourcesCompat.getColor(context.resources, it, null) }.sorted()

    fun getLuminance(color: Int): Float {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Color.luminance(color)
        } else {
            // Source: https://stackoverflow.com/a/596241
            val luminance: Double = (Color.red(color) * 0.2126) + (Color.green(color) * 0.7152) + (Color.blue(color) * 0.0722)
            luminance.toFloat()
        }
    }

    // Luminance >= 0.6: Black text, otherwise white
    fun getTextColorForBackgroundColor(backgroundColor: Int) =
            if (getLuminance(backgroundColor) >= 0.6)
                ResourcesCompat.getColor(context.resources, R.color.black, null) else
                ResourcesCompat.getColor(context.resources, R.color.white, null)
}