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
    private val codec: CodecPool.Codec? = null) {

    private var eosReached = false
    private var extractorDone = false
    private var lastInputStatus: ProcessInputStatus? = null
    private var lastOutputStatus: ProcessOutputStatus? = null
    private var outputRetries = 0
    private var totalSize = 0

    suspend fun extractBuffer(): ByteBuffer? {
        return when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractBufferRaw()
            else -> codec?.let { extractBufferEncoded(it, false).buffer }
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
            var outputResult: ProcessOutputResult?
            do {
                val result = extractBufferEncoded(codec, true)
                val sampleSize =
                    if (result.status == ProcessOutputStatus.SUCCESS && result.buffer != null) {
                        buffer.put(result.buffer)
                        result.buffer.position()
                    } else 0
                totalSize += sampleSize
                outputResult = result
                if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                    "$this prime: outputResult=$outputResult, sampleSize=$sampleSize, totalSize=$totalSize, lastOutputStatus=$lastOutputStatus, outputRetries=$outputRetries")
            } while (totalSize + sampleSize <= bufferSize && outputResult?.status == ProcessOutputStatus.SUCCESS)
            if (outputResult?.status != ProcessOutputStatus.SUCCESS && outputResult?.status != ProcessOutputStatus.EOS) {
                if (BuildConfig.DEBUG) Log.d(AudioFile.LOG_TAG,
                    "PRIMETEST: prime(), this=$this, outputResult=$outputResult, codec=$codec, extractor=$extractor, lastInputStatus=$lastInputStatus")
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                return null
            }
            return buffer.limit(buffer.position()).rewind() as ByteBuffer
        }
        return null
    }

    private fun extractBufferRaw(): ByteBuffer? {
        return if (!extractorDone) {
            try {
                val buffer = ByteBuffer.allocate(bufferSize)
                extractor.readSampleData(buffer, 0)
                extractorDone = !extractor.advance()
                eosReached = extractorDone
                buffer
            } catch (e: Exception) {
                Log.e(LOG_TAG, "extractBufferRaw", e)
                null
            }
        } else null
    }

    private suspend fun extractBufferEncoded(codec: CodecPool.Codec, priming: Boolean): ProcessOutputResult {
        val job = coroutineContext[Job]
        if (job?.isActive == true && (lastOutputStatus == null || lastOutputStatus == ProcessOutputStatus.SUCCESS || outputRetries++ < 5)) {
            if (lastInputStatus != ProcessInputStatus.END)
                lastInputStatus = processInputBuffer(codec)
            if (!job.isActive) return ProcessOutputResult(ProcessOutputStatus.INTERRUPTED, null)
            val outputResult = processOutputBuffer(codec, priming)
            if (priming && BuildConfig.DEBUG) Log.d(AudioFile.LOG_TAG,
                "PRIMETEST: extractBufferEncoded(), this=$this, outputResult=$outputResult, codec=$codec, extractor=$extractor, lastInputStatus=$lastInputStatus")
            lastOutputStatus = outputResult.status
            if ((outputResult.status == ProcessOutputStatus.SUCCESS || outputResult.status == ProcessOutputStatus.EOS) && outputResult.buffer != null) {
                totalSize += outputResult.buffer.remaining()
                outputRetries = 0
                eosReached = outputResult.status == ProcessOutputStatus.EOS
            }
            return outputResult
        }
        return ProcessOutputResult(ProcessOutputStatus.UNKNOWN, null)
    }

    private fun processInputBuffer(codec: CodecPool.Codec): ProcessInputStatus {
        if (BuildConfig.DEBUG) Functions.warnIfOnMainThread("processInputBuffer")

        try {
            val index = codec.dequeueInputBuffer(1000L)
            if (index != null && index >= 0) {
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

    private fun processOutputBuffer(codec: CodecPool.Codec, priming: Boolean): ProcessOutputResult {
        if (BuildConfig.DEBUG) Functions.warnIfOnMainThread("processOutputBuffer")

        val info = MediaCodec.BufferInfo()

        try {
            val index = codec.dequeueOutputBuffer(info, if (priming) TIMEOUT_PRIMING else TIMEOUT_REGULAR)
            @Suppress("DEPRECATION")
            when {
                index != null && index >= 0 -> {
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
                    codec.outputFormat.let { audioTrack.rebuild(Functions.mediaFormatToAudioFormat(it)) }
                    return processOutputBuffer(codec, priming)
                }
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return ProcessOutputResult(ProcessOutputStatus.TIMEOUT,
                    null)
                index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    /** If buffers changed, just try again? */
                    if (BuildConfig.DEBUG) Log.d(AudioFile.LOG_TAG,
                        "PRIMETEST: processOutputBuffer() INFO_OUTPUT_BUFFERS_CHANGED, this=$this, codec=$codec, extractor=$extractor, lastInputStatus=$lastInputStatus")
                    return processOutputBuffer(codec, priming)
                }
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
    enum class ProcessOutputStatus { SUCCESS, CODEC_CONFIG, NO_BUFFER, EOS, ERROR, TIMEOUT, INTERRUPTED, UNKNOWN }

    companion object {
        const val LOG_TAG = "AudioExtractor"

        // Timeout is in microseconds, so 1000 == 1 millisecond == 0.001 seconds
        const val TIMEOUT_REGULAR = 1_000L
        const val TIMEOUT_PRIMING = 1_000_000L
    }
}