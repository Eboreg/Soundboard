package us.huseli.soundboard.helpers

import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import us.huseli.soundboard.R

class ColorHelper(private val resources: Resources) {
    private val colorResources = arrayListOf(
        R.color.amber_500,
        R.color.black,
        R.color.blue_500,
        R.color.blue_grey_500,
        R.color.brown_500,
        R.color.cyan_500,
        R.color.deep_orange_500,
        R.color.deep_purple_500,
        R.color.green_500,
        R.color.grey_500,
        R.color.indigo_500,
        R.color.light_blue_500,
        R.color.light_green_500,
        R.color.lime_500,
        R.color.orange_500,
        R.color.pink_500,
        R.color.purple_500,
        R.color.red_500,
        R.color.teal_500,
        R.color.white,
        R.color.yellow_500)
    val colors = colorResources.map { getColor(it) }.sorted()

    /********** PRIVATE METHODS **********/
    private fun getColor(colorResId: Int) = ResourcesCompat.getColor(resources, colorResId, null)

    private fun getColorStringFromInt(color: Int) = String.format("%06X", 0xFFFFFF.and(color))

    /********** PUBLIC METHODS **********/
    fun getColorOnBackgroundColor(backgroundColor: Int) =
        // Luminance >= 0.6: Black text, otherwise white
        getColor(if (getLuminance(backgroundColor) >= 0.6) R.color.black else R.color.white)

    fun getColorStringFromAttr(attrResId: Int, theme: Resources.Theme): String? {
        val attr = TypedValue()
        theme.resolveAttribute(attrResId, attr, true)
        return if (attr.type >= TypedValue.TYPE_FIRST_COLOR_INT && attr.type <= TypedValue.TYPE_LAST_COLOR_INT)
            getColorStringFromInt(attr.data) else null
    }

    fun getLuminance(color: Int): Float {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Color.luminance(color)
        } else {
            // Source: https://stackoverflow.com/a/596241
            val luminance: Double =
                (Color.red(color) * 0.2126) + (Color.green(color) * 0.7152) + (Color.blue(color) * 0.0722)
            luminance.toFloat()
        }
    }

    fun getRandomColor(exclude: List<Int>): Int {
        val included = colors.filter { it !in exclude }
        return if (included.isNotEmpty()) included.random() else colors.random()
    }

    fun getSecondaryColorOnBackgroundColor(backgroundColor: Int) =
        getColor(if (getLuminance(backgroundColor) >= 0.6) R.color.light_bg_dark_secondary_text else R.color.dark_bg_light_secondary_text)
}