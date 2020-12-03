package us.huseli.soundboard.data

import android.net.Uri
import androidx.room.TypeConverter

object Converters {
    @TypeConverter
    @JvmStatic
    fun uriToString(uri: Uri): String = uri.toString()

    @TypeConverter
    @JvmStatic
    fun stringToUri(str: String): Uri = Uri.parse(str)

}