package us.huseli.soundboard_kotlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Sound::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SoundDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao

    companion object {
        @Volatile private var instance: SoundDatabase? = null

        fun getInstance(context: Context): SoundDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SoundDatabase {
            return Room.databaseBuilder(context, SoundDatabase::class.java, "sound_database")
                    .fallbackToDestructiveMigration().build()
        }
    }
}