package us.huseli.soundboard.helpers

import android.content.res.Resources
import android.graphics.Color
import androidx.core.content.res.ResourcesCompat
import us.huseli.soundboard.R

class ColorHelper(private val resources: Resources) {
    private val colorResources = arrayListOf(R.color.amber_500, R.color.black, R.color.blue_500, R.color.blue_grey_500,
            R.color.brown_500, R.color.cyan_500, R.color.deep_orange_500, R.color.deep_purple_500, R.color.green_500, R.color.grey_500,
            R.color.indigo_500, R.color.light_blue_500, R.color.light_green_500, R.color.lime_500, R.color.orange_500, R.color.pink_500,
            R.color.purple_500, R.color.red_500, R.color.teal_500, R.color.white, R.color.yellow_500)
    val colors = colorResources.map { ResourcesCompat.getColor(resources, it, null) }.sorted()

    fun randomColor(exclude: List<Int>): Int {
        val included = colors.filter { it !in exclude }
        return if (included.isNotEmpty()) included.random() else colors.random()
    }

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
    fun getColorOnBackgroundColor(backgroundColor: Int) =
            if (getLuminance(backgroundColor) >= 0.6)
                ResourcesCompat.getColor(resources, R.color.black, null) else
                ResourcesCompat.getColor(resources, R.color.white, null)

    fun getSecondaryColorOnBackgroundColor(backgroundColor: Int) =
            if (getLuminance(backgroundColor) >= 0.6)
                ResourcesCompat.getColor(resources, R.color.light_bg_dark_secondary_text, null) else
                ResourcesCompat.getColor(resources, R.color.dark_bg_light_secondary_text, null)
}