package us.huseli.soundboard_kotlin.helpers

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import us.huseli.soundboard_kotlin.R

class ColorHelper(val context: Context) {
    private val colorResources = arrayListOf(R.color.amber_500, R.color.black, R.color.blue_500, R.color.blue_grey_500,
            R.color.brown_500, R.color.cyan_500, R.color.deep_orange_500, R.color.deep_purple_500, R.color.green_500, R.color.grey_500,
            R.color.indigo_500, R.color.light_blue_500, R.color.light_green_500, R.color.lime_500, R.color.orange_500, R.color.pink_500,
            R.color.purple_500, R.color.red_500, R.color.teal_500, R.color.white, R.color.yellow_500)
    val colors = ArrayList(colorResources.map { ResourcesCompat.getColor(context.resources, it, null) })
    val colorStrings = ArrayList(colorResources.map { context.resources.getString(it) })
}