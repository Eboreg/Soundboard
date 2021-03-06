package us.huseli.soundboard.audio

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaFormat
import android.os.Build
import us.huseli.soundboard.data.Constants
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.Functions
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTrackProvider @Inject constructor() {
    private var baseBufferSize = Constants.DEFAULT_BUFFER_SIZE
    private val containers = Collections.synchronizedList(mutableListOf<AudioTrackContainer>())
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    fun acquire(sound: Sound, mediaFormat: MediaFormat): AudioTrack? {
        return synchronized(containers) { containers.find { it.sound == sound }?.audioTrack }
            ?: createIfPossible(sound, mediaFormat)
    }

    fun release(sound: Sound) {
        synchronized(containers) {
            containers.find { it.sound == sound }?.also {
                it.sound = null
                it.audioTrack.release()
            }
        }
    }

    private fun createIfPossible(sound: Sound, mediaFormat: MediaFormat): AudioTrack? {
        // 1. If there are vacant, usable AudioTracks, use the first one
        return synchronized(containers) {
            containers.find { it.sound == null && it.formatMatches(mediaFormat) }?.let {
                it.sound = sound
                it.audioTrack
            } ?: run {
                // 2. If max number of AudioTracks is not reached, create a new one
                if (containers.size < Constants.MAX_AUDIOTRACK_COUNT) {
                    AudioTrackContainer(sound, mediaFormat).let {
                        containers.add(it)
                        it.audioTrack
                    }
                } else null
            }
        }
    }

    inner class AudioTrackContainer(sound: Sound, mediaFormat: MediaFormat) {
        val audioTrack: AudioTrack
        var sound: Sound? = sound

        private val audioFormat = Functions.mediaFormatToAudioFormat(mediaFormat)
        private val bufferSize = baseBufferSize * mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        init {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val builder = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                builder.build()
            } else AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        }

        fun formatMatches(mediaFormat: MediaFormat): Boolean {
            return Functions.mediaFormatToAudioFormat(mediaFormat).run {
                sampleRate == audioFormat.sampleRate && channelMask == audioFormat.channelMask && encoding == audioFormat.encoding
            }
        }
    }
}