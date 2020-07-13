package us.huseli.soundboard_kotlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Sound::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SoundDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao

    companion object {
        @Volatile private var instance: SoundDatabase? = null

        private val MIGRATION_4_5 = object: Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Sound ADD COLUMN volume REAL NOT NULL DEFAULT 1.0")
            }
        }

        private val MIGRATION_5_6 = object: Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE Sound_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        'order' INTEGER NOT NULL,
                        volume INTEGER NOT NULL
                    )
                    """.trimIndent())
                database.execSQL("""
                    INSERT INTO Sound_new (id, name, uri, 'order', volume)
                    SELECT id, name, uri, 'order', CAST(volume * 100 as INTEGER) FROM Sound
                    """.trimIndent())
                database.execSQL("DROP TABLE Sound")
                database.execSQL("ALTER TABLE Sound_new RENAME TO Sound")
            }
        }

        fun getInstance(context: Context): SoundDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SoundDatabase {
            return Room.databaseBuilder(context, SoundDatabase::class.java, "sound_database")
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
        }
    }
}