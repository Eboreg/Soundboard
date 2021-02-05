package us.huseli.soundboard.data

import androidx.room.TypeConverter
import java.util.*

object Converters {
    @TypeConverter
    @JvmStatic
    fun longToDate(value: Long): Date = Date(value)

    @TypeConverter
    @JvmStatic
    fun dateToLong(value: Date): Long = value.time
}