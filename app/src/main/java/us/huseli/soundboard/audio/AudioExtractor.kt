package us.huseli.soundboard.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Job
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.helpers.Functions
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext

class AudioExtractor(
    private val audioTrack: AudioTrackContainer,
    private val extractor: MediaExtractor,
    private val mime: String,
    private val bufferSize: Int,
    private val codec: MediaCodec? = null) {

    private var eosReached = false
    private var extractorDone = false
    private var lastInputStatus: ProcessInputStatus? = null
    private var lastOutputStatus: ProcessOutputStatus? = null
    private var outputRetries = 0
    private var totalSize = 0

    suspend fun extractBuffer(): ByteBuffer? {
        return when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractBufferRaw()
            else -> codec?.let { extractBufferEncoded(it, false)?.buffer }
        }
    }

    fun isEosReached() = eosReached

    suspend fun prime(): ByteBuffer? {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        if (mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
            do {
                val readSize = extractor.readSampleData(buffer, 0)
                extractorDone = !extractor.advance()
            } while (readSize == 0 && !extractorDone)
            return buffer
        } else codec?.also { codec ->
            do {
                val outputResult = extractBufferEncoded(codec, true)
                val sampleSize =
                    if (outputResult?.status == ProcessOutputStatus.SUCCESS && outputResult.buffer != null) {
                        buffer.put(outputResult.buffer)
                        outputResult.buffer.position()
                    } else 0
                totalSize += sampleSize
                if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                    "$this prime: outputResult=$outputResult, sampleSize=$sampleSize, totalSize=$totalSize, lastOutputStatus=$lastOutputStatus, outputRetries=$outputRetries")
            } while (
                totalSize + sampleSize <= bufferSize &&
                (outputResult?.status == ProcessOutputStatus.TIMEOUT || outputResult?.status == ProcessOutputStatus.SUCCESS))
            return buffer.limit(buffer.position()).rewind() as ByteBuffer
        }
        return null
    }

    private fun extractBufferRaw(): ByteBuffer? {
        return if (!extractorDone) {
            val buffer = ByteBuffer.allocate(bufferSize)
            extractor.readSampleData(buffer, 0)
            extractorDone = !extractor.advance()
            eosReached = extractorDone
            buffer
        } else null
    }

    private suspend fun extractBufferEncoded(codec: MediaCodec, priming: Boolean): ProcessOutputResult? {
        val job = coroutineContext[Job]
        if (job?.isActive == true && (lastOutputStatus == null || lastOutputStatus == ProcessOutputStatus.SUCCESS || outputRetries++ < 5)) {
            if (lastInputStatus != ProcessInputStatus.END)
                lastInputStatus = processInputBuffer(codec)
            if (!job.isActive) return null
            val outputResult = processOutputBuffer(codec, priming)
            lastOutputStatus = outputResult.status
            if ((outputResult.status == ProcessOutputStatus.SUCCESS || outputResult.status == ProcessOutputStatus.EOS) && outputResult.buffer != null) {
                totalSize += outputResult.buffer.remaining()
                outputRetries = 0
                eosReached = outputResult.status == ProcessOutputStatus.EOS
            }
            return outputResult
        }
        return null
    }

    private fun processInputBuffer(codec: MediaCodec): ProcessInputStatus {
        if (BuildConfig.DEBUG) Functions.warnIfOnMainThread("processInputBuffer")

        try {
            val index = codec.dequeueInputBuffer(1000L)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index)
                if (buffer != null) {
                    if (lastInputStatus == ProcessInputStatus.END_NEXT) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            codec.queueInputBuffer(
                                index, 0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                        }
                        extractorDone = !extractor.advance()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(AudioFile.LOG_TAG, "Error in codec input", e)
        }
        return when {
            lastInputStatus == ProcessInputStatus.END_NEXT -> ProcessInputStatus.END
            extractorDone -> ProcessInputStatus.END_NEXT
            else -> ProcessInputStatus.CONTINUE
        }
    }

    private fun processOutputBuffer(codec: MediaCodec, priming: Boolean): ProcessOutputResult {
        if (BuildConfig.DEBUG) Functions.warnIfOnMainThread("processOutputBuffer")

        val info = MediaCodec.BufferInfo()

        try {
            val index = codec.dequeueOutputBuffer(info, if (priming) TIMEOUT_PRIMING else TIMEOUT_REGULAR)
            when {
                index >= 0 -> {
                    val buffer = codec.getOutputBuffer(index)
                    return when {
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 -> {
                            codec.releaseOutputBuffer(index, false)
                            ProcessOutputResult(ProcessOutputStatus.CODEC_CONFIG, null)
                        }
                        buffer != null -> {
                            val outputBuffer = ByteBuffer.allocateDirect(buffer.remaining()).put(buffer)
                            outputBuffer.rewind()
                            codec.releaseOutputBuffer(index, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                                ProcessOutputResult(ProcessOutputStatus.EOS, outputBuffer)
                            else
                                ProcessOutputResult(ProcessOutputStatus.SUCCESS, outputBuffer)
                        }
                        else -> ProcessOutputResult(ProcessOutputStatus.NO_BUFFER, null)
                    }
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    /** If format changed, apply changes and do this method once again */
                    audioTrack.rebuild(Functions.mediaFormatToAudioFormat(codec.outputFormat))
                    return processOutputBuffer(codec, priming)
                }
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return ProcessOutputResult(ProcessOutputStatus.TIMEOUT,
                    null)
                else -> return ProcessOutputResult(ProcessOutputStatus.UNKNOWN, null)
            }
        } catch (e: Exception) {
            Log.e(AudioFile.LOG_TAG, "Error in codec output", e)
            return ProcessOutputResult(ProcessOutputStatus.ERROR, null)
        }
    }

    enum class ProcessInputStatus { CONTINUE, END_NEXT, END }

    data class ProcessOutputResult(val status: ProcessOutputStatus, val buffer: ByteBuffer?)

    /**
     * CODEC_CONFIG = "This indicated that the buffer marked as such contains codec initialization / codec specific
     * data instead of media data."
     */
    enum class ProcessOutputStatus { SUCCESS, CODEC_CONFIG, NO_BUFFER, EOS, ERROR, TIMEOUT, UNKNOWN }

    companion object {
        const val LOG_TAG = "AudioExtractor"
        const val TIMEOUT_REGULAR = 1_000L
        const val TIMEOUT_PRIMING = 20_000L
    }
}