package us.huseli.soundboard.data

object Constants {
    const val DEFAULT_BUFFER_SIZE = 44100  // half or double this if needed
    const val DEFAULT_SPANCOUNT_LANDSCAPE = 8
    const val DEFAULT_SPANCOUNT_PORTRAIT = 4
    const val DEFAULT_VOLUME = 100
    const val MAX_UNDO_STATES = 20
    const val MINIMUM_SAMPLE_SIZE = 75000
    const val PREF_LAST_VERSION = "lastRunVersionCode"
    const val PREF_REPRESS_MODE = "repressMode"
    const val SOUND_DIRNAME = "sounds"
    const val SOUND_PLAY_TIMEOUT = 500_000_000L  // nanoseconds
}