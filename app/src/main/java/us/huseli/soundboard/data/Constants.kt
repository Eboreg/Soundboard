package us.huseli.soundboard.data

object Constants {
    const val BACKUP_TEMP_DIRNAME = "backup_temp"
    const val DATABASE_NAME = "sound_database"
    const val DATABASE_TEMP_NAME = "temp_sound_database.db"
    const val DEFAULT_BUFFER_SIZE = 44100  // half or double this if needed
    const val DEFAULT_LANGUAGE = "default"
    const val DEFAULT_SPANCOUNT_LANDSCAPE = 8
    const val DEFAULT_SPANCOUNT_PORTRAIT = 4
    const val DEFAULT_VOLUME = 100
    const val MAX_UNDO_STATES = 20

    // const val MINIMUM_SAMPLE_SIZE = 75000
    const val PREF_BUFFER_SIZE = "bufferSize"
    const val PREF_LANDSCAPE_SPAN_COUNT = "landscapeSpanCount"
    const val PREF_LANGUAGE = "language"
    const val PREF_LAST_VERSION = "lastRunVersionCode"
    const val PREF_NIGHT_MODE = "nightMode"
    const val PREF_REPRESS_MODE = "repressMode"

    const val SOUND_DIRNAME = "sounds"
    const val SOUND_PLAY_TIMEOUT = 500_000_000L  // nanoseconds
    const val ZIP_BUFFER_SIZE = 2048
    const val ZIP_DB_DIR = "database"
    const val ZIP_PREFS_FILENAME = "preferences.json"
    const val ZIP_SOUNDS_DIR = "sounds"
}