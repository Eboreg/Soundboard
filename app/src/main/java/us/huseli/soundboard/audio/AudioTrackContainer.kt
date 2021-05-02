package us.huseli.soundboard.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.helpers.Functions
import java.nio.ByteBuffer

class AudioTrackContainer(
    /** Volume = float, 0.0 - 1.0 */
    private val audioAttributes: AudioAttributes,
    private var outputAudioFormat: AudioFormat,
    private var bufferSize: Int,
    private var volume: Float) {

    constructor(audioAttributes: AudioAttributes, outputAudioFormat: AudioFormat, bufferSize: Int, volume: Int) :
            this(audioAttributes, outputAudioFormat, bufferSize, volume.toFloat() / 100)

    private var audioTrack: AudioTrack? = null
    private var startAtPosition = 0

    val playbackHeadPosition: Int
        get() = audioTrack?.playbackHeadPosition ?: 0

    suspend fun awaitPausedState() {
        while (audioTrack != null && audioTrack?.playState != AudioTrack.PLAYSTATE_PAUSED) delay(10)
    }

    fun build(): AudioTrackContainer {
        startAtPosition = 0
        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(outputAudioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            builder.build()
        } else AudioTrack(
            audioAttributes,
            outputAudioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.setVolume(volume)
        audioTrack = track
        return this
    }

    fun flush(): AudioTrackContainer {
        stop()
        audioTrack?.flush()
        return this
    }

    fun pause(): AudioTrackContainer {
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
        }
        startAtPosition = playbackHeadPosition
        return this
    }

    fun play(): AudioTrackContainer {
        audioTrack?.play()
        return this
    }

    fun rebuild(audioFormat: AudioFormat): AudioTrackContainer {
        if (!Functions.audioFormatsEqual(audioFormat, outputAudioFormat)) {
            outputAudioFormat = audioFormat
            return release().build()
        }
        return this
    }

    fun release(): AudioTrackContainer {
        stop()
        audioTrack?.release()
        audioTrack = null
        return this
    }

    fun resume(): AudioTrackContainer {
        audioTrack?.play()
        return this
    }

    fun setBufferSize(value: Int): AudioTrackContainer {
        bufferSize = value
        return this
    }

    fun setVolume(value: Int): AudioTrackContainer {
        volume = value.toFloat() / 100
        return this
    }

    fun stop() {
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
        }
        startAtPosition = 0
    }

    fun write(buffer: ByteBuffer?): WriteResult {
        /**
         * We assume that sound is playing when write() is called. Therefore, if data has been written but
         * playback head position has not changed, something is not working and we till try to recreate the AudioTrack
         * and restart the playback, and hope for better luck next time.
         *
         * More precisely: If data was written AND playback head position is unchanged AND overshoot has happened
         * (i.e. AudioTrack buffer is full), retry write max 5 times. If the problem persists, return a WriteResult
         * with status == FAIL.
         *
         * Will only ever return results with status: NO_BUFFER, OK, ERROR, FAIL
         */
        if (buffer == null) return WriteResult.create(WriteStatus.NO_BUFFER)

        val sampleSize = buffer.remaining()
        var retries = 0

        var result = internalWrite(buffer, sampleSize)
        while (result.status == WriteStatus.OVERSHOOT && playbackHeadPosition == startAtPosition && retries++ < 5) {
            if (BuildConfig.DEBUG) Log.w(LOG_TAG,
                "write: overshoot happened and playbackHeadPosition is still $playbackHeadPosition, retries=$retries; trying again")
            // Rewind buffer to byte after last successfully written (maybe not really needed?)
            buffer.position(result.writtenBytes)
            result = internalWrite(buffer, sampleSize)
        }
        // If we tried 5 times but are still at the start position
        if (result.status == WriteStatus.OVERSHOOT && playbackHeadPosition == startAtPosition) {
            if (BuildConfig.DEBUG) Log.w(LOG_TAG,
                "write: tried 5 times and playbackHeadPosition is still $playbackHeadPosition; I give up! writtenBytes=${result.writtenBytes}, sampleSize=${result.sampleSize}, buffer=$buffer")
            return WriteResult.create(WriteStatus.FAIL)
        }
        return result
    }

    private fun internalWrite(buffer: ByteBuffer, sampleSize: Int): WriteResult {
        return audioTrack?.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING)?.let { result ->
            WriteResult.create(result, sampleSize)
        } ?: WriteResult.create(null, sampleSize)
    }

    class WriteResult(
        val status: WriteStatus,
        audioTrackResult: Int?,
        val sampleSize: Int,
        val overshoot: Int) {

        val message = when (audioTrackResult) {
            AudioTrack.ERROR_BAD_VALUE -> "Audio output: bad value"
            AudioTrack.ERROR_INVALID_OPERATION -> "Audio output: not properly initialized"
            AudioTrack.ERROR_DEAD_OBJECT -> "Audio output: dead object"
            AudioTrack.ERROR -> "Error outputting audio"
            else -> when (status) {
                WriteStatus.OVERSHOOT -> "Overshoot"
                WriteStatus.FAIL -> "Fail"
                WriteStatus.NO_BUFFER -> "No buffer sent"
                WriteStatus.ERROR -> "Audio track not initialized"
                else -> "OK"
            }
        }
        val writtenBytes = if (audioTrackResult != null && audioTrackResult >= 0) audioTrackResult else 0

        override fun toString(): String {
            return "WriteResult <status=$status, sampleSize=$sampleSize, overshoot=$overshoot>"
        }

        companion object {
            fun create(audioTrackResult: Int?, sampleSize: Int): WriteResult {
                val overshoot =
                    if (audioTrackResult != null && audioTrackResult >= 0) sampleSize - audioTrackResult else 0
                val status = when {
                    audioTrackResult == null || audioTrackResult < 0 -> WriteStatus.ERROR
                    overshoot > 0 -> WriteStatus.OVERSHOOT
                    else -> WriteStatus.OK
                }
                return WriteResult(status, audioTrackResult, sampleSize, overshoot)
            }

            fun create(status: WriteStatus) = WriteResult(status, null, 0, 0)
        }
    }

    enum class WriteStatus { OK, OVERSHOOT, ERROR, FAIL, NO_BUFFER }

    companion object {
        const val LOG_TAG = "AudioTrackContainer"
    }
}
