package us.huseli.soundboard.data

object Constants {
    const val SOUND_DIRNAME = "sounds"
    const val DEFAULT_BUFFER_SIZE = 44100  // half or double this if needed
    const val DEFAULT_VOLUME = 100
    const val MINIMUM_SAMPLE_SIZE = 75000
    const val SOUND_PLAY_TIMEOUT = 500_000_000L  // nanoseconds
}