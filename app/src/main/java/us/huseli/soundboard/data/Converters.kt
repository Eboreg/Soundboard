package us.huseli.soundboard.data

import android.net.Uri
import androidx.room.TypeConverter
import java.util.*

object Converters {
    @TypeConverter
    @JvmStatic
    fun uriToString(uri: Uri): String = uri.toString()

    @TypeConverter
    @JvmStatic
    fun stringToUri(str: String): Uri = Uri.parse(str)

    @TypeConverter
    @JvmStatic
    fun longToDate(value: Long): Date = Date(value)

    @TypeConverter
    @JvmStatic
    fun dateToLong(value: Date): Long = value.time
}