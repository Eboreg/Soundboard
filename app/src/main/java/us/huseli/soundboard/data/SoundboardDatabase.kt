package us.huseli.soundboard.data

import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.util.*

@Database(entities = [Sound::class, Category::class], version = 16, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SoundboardDatabase : RoomDatabase() {
    abstract fun soundDao(): SoundDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Sound ADD COLUMN volume REAL NOT NULL DEFAULT 1.0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE Sound_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        'order' INTEGER NOT NULL,
                        volume INTEGER NOT NULL
                    )""".trimIndent())
                database.execSQL("""
                    INSERT INTO Sound_new (id, name, uri, 'order', volume)
                    SELECT id, name, uri, 'order', CAST(volume * 100 as INTEGER) FROM Sound
                    """.trimIndent())
                database.execSQL("DROP TABLE Sound")
                database.execSQL("ALTER TABLE Sound_new RENAME TO Sound")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE SoundCategory (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        backgroundColor INTEGER NOT NULL,
                        textColor INTEGER NOT NULL,
                        'order' INTEGER NOT NULL
                    )""".trimIndent())
                database.execSQL("""
                    CREATE TABLE Sound_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        'order' INTEGER NOT NULL,
                        volume INTEGER NOT NULL,
                        categoryId INTEGER,
                        FOREIGN KEY (categoryId) REFERENCES SoundCategory(id) ON UPDATE SET NULL ON DELETE SET NULL
                    )""".trimIndent())
                database.execSQL("""
                    INSERT INTO Sound_new (id, name, uri, 'order', volume, categoryId)
                    SELECT id, name, uri, 'order', volume, NULL FROM Sound
                    """.trimIndent())
                database.execSQL("DROP TABLE Sound")
                database.execSQL("ALTER TABLE Sound_new RENAME TO Sound")
                database.execSQL("CREATE INDEX index_Sound_categoryId ON Sound(categoryId)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE SoundCategory_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        backgroundColor INTEGER NOT NULL,
                        'order' INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO SoundCategory_new (id, name, backgroundColor, 'order')
                    SELECT id, name, backgroundColor, 'order' FROM SoundCategory
                """.trimIndent())
                database.execSQL("DROP INDEX index_Sound_categoryId")
                database.execSQL("DROP TABLE SoundCategory")
                database.execSQL("ALTER TABLE SoundCategory_new RENAME TO SoundCategory")
                database.execSQL("CREATE INDEX index_Sound_categoryId ON Sound(categoryId)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            // Sets ON UPDATE and ON DELETE on Sound
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE Sound_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        'order' INTEGER NOT NULL,
                        volume INTEGER NOT NULL,
                        categoryId INTEGER,
                        FOREIGN KEY (categoryId) REFERENCES SoundCategory(id) ON UPDATE CASCADE ON DELETE CASCADE
                    )""".trimIndent())
                database.execSQL("""
                    INSERT INTO Sound_new (id, name, uri, 'order', volume, categoryId)
                    SELECT id, name, uri, 'order', volume, categoryId FROM Sound
                    """.trimIndent())
                database.execSQL("DROP TABLE Sound")
                database.execSQL("ALTER TABLE Sound_new RENAME TO Sound")
                database.execSQL("CREATE INDEX index_Sound_categoryId ON Sound(categoryId)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE SoundCategory ADD COLUMN collapsed INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val now = Date().time
                database.execSQL("ALTER TABLE Sound ADD COLUMN added INTEGER NOT NULL DEFAULT $now")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Sound ADD COLUMN duration INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Sound ADD COLUMN checksum TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE Sound_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        path TEXT NOT NULL,
                        'order' INTEGER NOT NULL,
                        volume INTEGER NOT NULL,
                        categoryId INTEGER,
                        duration INTEGER NOT NULL,
                        added INTEGER NOT NULL,
                        checksum TEXT,
                        FOREIGN KEY (categoryId) REFERENCES SoundCategory(id) ON UPDATE CASCADE ON DELETE CASCADE
                    )""".trimIndent())
                val cursor = database.query("SELECT * FROM Sound")
                while (cursor.moveToNext()) {
                    val sound = Sound(
                        cursor.getInt(0),                       // id
                        cursor.getInt(1),                       // categoryId
                        cursor.getString(2),                    // name
                        Uri.parse(cursor.getString(3)).path!!,  // path
                        cursor.getInt(4),                       // order
                        cursor.getInt(5),                       // volume
                        Date(cursor.getLong(6)),                // added
                        cursor.getLong(7),                      // duration
                        cursor.getString(8) ?: "",
                        false
                    )
                    val query = """
                        INSERT INTO Sound_new (id, categoryId, name, path, 'order', volume, checksum, added, duration)
                        VALUES (${sound.id}, ${sound.categoryId}, '${sound.name.replace("'", "''")}', 
                        '${sound.path}', ${sound.order}, ${sound.volume}, '${sound.checksum}', ${sound.added.time}, 
                        ${sound.duration})
                    """.trimIndent()
                    database.execSQL(query)
                }
                database.execSQL("DROP TABLE Sound")
                database.execSQL("ALTER TABLE Sound_new RENAME TO Sound")
                database.execSQL("CREATE INDEX index_Sound_categoryId ON Sound(categoryId)")
            }
        }

        private val MIGRATION_14_16 = object : Migration(14, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Alter ON DELETE for categoryId, add trashed column
                val now = Date().time
                database.execSQL("""
                    CREATE TABLE Sound_new (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        'order' INTEGER NOT NULL,
                        volume INTEGER NOT NULL,
                        categoryId INTEGER,
                        added INTEGER NOT NULL DEFAULT $now,
                        duration INTEGER NOT NULL DEFAULT -1,
                        checksum TEXT DEFAULT NULL,
                        trashed INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (categoryId) REFERENCES SoundCategory(id) ON UPDATE CASCADE ON DELETE SET NULL
                    )""".trimIndent())
                database.execSQL("""
                    INSERT INTO Sound_new (id, name, uri, 'order', volume, categoryId, added, duration, checksum)
                    SELECT id, name, uri, 'order', volume, categoryId, added, duration, checksum FROM Sound
                """.trimIndent())
                database.execSQL("DROP TABLE Sound")
                database.execSQL("ALTER TABLE Sound_new RENAME TO Sound")
                database.execSQL("CREATE INDEX index_Sound_categoryId ON Sound(categoryId)")
                // Add autoImportCategory column to SoundCategory
                database.execSQL("ALTER TABLE SoundCategory ADD COLUMN autoImportCategory INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun buildFromFile(context: Context, file: File, dbName: String): SoundboardDatabase {
            return Room.databaseBuilder(context, SoundboardDatabase::class.java, dbName)
                .createFromFile(file)
                .build()
        }

        fun build(context: Context): SoundboardDatabase {
            return Room.databaseBuilder(context, SoundboardDatabase::class.java, Constants.DATABASE_NAME)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(MIGRATION_9_10)
                .addMigrations(MIGRATION_10_11)
                .addMigrations(MIGRATION_11_12)
                .addMigrations(MIGRATION_12_13)
                .addMigrations(MIGRATION_13_14)
                .addMigrations(MIGRATION_14_16)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}