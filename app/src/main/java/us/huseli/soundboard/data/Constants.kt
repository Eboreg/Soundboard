package us.huseli.soundboard.data

object Constants {
    const val BACKUP_TEMP_DIRNAME = "backup_temp"
    const val DATABASE_NAME = "sound_database"
    const val DATABASE_TEMP_NAME = "temp_sound_database.db"
    const val DEFAULT_BUFFER_SIZE = 44100  // half or double this if needed
    const val DEFAULT_LANGUAGE = "default"
    const val DEFAULT_SPANCOUNT_LANDSCAPE = 8
    // const val DEFAULT_SPANCOUNT_PORTRAIT = 4
    const val DEFAULT_VOLUME = 100
    const val MAX_UNDO_STATES = 20

    // const val MINIMUM_SAMPLE_SIZE = 75000

    const val SOUND_DIRNAME = "sounds"
    const val SOUND_PLAY_TIMEOUT = 500_000_000L  // nanoseconds = 0.5 seconds
    const val ZIP_BUFFER_SIZE = 2048
    const val ZIP_DB_DIR = "database"
    const val ZIP_PREFS_FILENAME = "preferences.json"
    const val ZIP_SOUNDS_DIR = "sounds"
}