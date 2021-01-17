package us.huseli.soundboard.helpers

import android.media.*
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import kotlin.math.roundToLong

@RequiresApi(Build.VERSION_CODES.O)
open class AudioFileBackup(private val path: String, private val name: String) : Runnable {
    /** name is really only for debugging purposes */
    private val audioAttributes: AudioAttributes
    private val channelCount: Int
    private val codecCallback = CodecCallback()
    private val extractor = MediaExtractor().apply { setDataSource(path) }
    private val inputFormat: MediaFormat
    private val mime: String
    private val trackNumber: Int

    private var audioTrack: AudioTrack
    private var codec: MediaCodec? = null
    private var extractMode = ExtractMode.PRIME
    private var outputFormat: AudioFormat
    private var state = State.UNINITIALIZED

    init {
        val mediaFormatPair = getMediaFormat() ?: throw Exception("Could not do getMediaFormat()")
        trackNumber = mediaFormatPair.first
        inputFormat = mediaFormatPair.second
        mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw Exception("Could not get MIME type")
        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        val channelMask = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        outputFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelMask)
                .build()
        audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setBufferSizeInBytes(BUFFER_SIZE)
                .setAudioFormat(outputFormat)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

        extractor.selectTrack(trackNumber)
        prime()

        Log.d(LOG_TAG, "init: name=$name, path=$path, mime=$mime, audioTrack=$audioTrack, outputFormat=$outputFormat, audioAttributes=$audioAttributes")
    }

    private fun initCodec() {
        val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(inputFormat)
                ?: throw Exception("Could not find suitable codec")
        codecCallback.setOnEndOfOutputStreamListener { lastSampleSize ->
            stop(bytesToMilliseconds(lastSampleSize, outputFormat.sampleRate, channelCount))
        }
        codec = MediaCodec.createByCodecName(codecName).also {
            it.setCallback(codecCallback)
            it.configure(inputFormat, null, null, 0)
            //it.start()
        }
    }

    fun play() {
        // TODO: Do this on SoundPlayer thread instead?
        Thread(this).start()
    }

    private fun bytesToMilliseconds(byteCount: Int, sampleRate: Int, channelCount: Int): Long {
        /**
         * Logic:
         * 1 second at 44100 Hz, 16 bit = 705600 bits = 88200 bytes
         * 1 second at 44100 Hz, 16 bit stereo = 88200 * 2 = 176400 bytes
         * That is:
         * bytes = seconds * hz * (bitdepth / 8) * channels
         * So, in reverse:
         * milliseconds = 1000 * bytes / (hz * (bitdepth / 8) * channels)
         */
        return ((1000.0 * byteCount) / (sampleRate * 2 * channelCount).toDouble()).roundToLong()
    }

    fun stop(delay: Long) {
        /** delay = milliseconds */
        if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            // "When used on an instance created in MODE_STREAM mode, audio will stop playing
            // after the last buffer that was written has been played."
            audioTrack.stop()
            Thread.sleep(delay)
            // Now we should be free to flush the audioTrack or whatever
            audioTrack.flush()
            //codec?.flush()
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            prime()
        }
    }

    private fun prime() {
        /**
         * Pre-load audioTrack with some reasonable amount of data for low latency.
         * Expects extractor to be ready and positioned at beginning of track.
         */
        extractMode = ExtractMode.PRIME
        state = State.INITIALIZING
        startCodec()
        extract(PRIMING_SIZE)
        state = State.READY
    }

    private fun extract() = extract(null)

    private fun extract(maxSize: Int?): Int? {
        /**
         * Extracts sample data from extractor and feeds it to audioTrack.
         * Before: Make sure extractor is positioned at the correct sample and audioTrack is ready
         * for writing.
         * Parameters:
         *   maxSize = Extract max this number of bytes
         * Returns: Size of last written sample or 0 if there were none.
         */
        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) {
            extractEncoded(maxSize)
            return null
        }

        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        var lastSampleSize: Int? = null
        var totalSize = 0
        var shouldQuit = false
        do {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize > 0) lastSampleSize = sampleSize
            if (sampleSize >= 0) {
                totalSize += sampleSize
                audioTrack.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING)
                buffer.clear()
                extractor.advance()
            }
            // Check maxSize against probable totalSize after _next_ iteration:
            if (sampleSize < 0 || (maxSize != null && totalSize + sampleSize > maxSize)) {
                shouldQuit = true
            }
        } while (!shouldQuit)
        return lastSampleSize ?: 0
    }

    fun extractEncoded(maxSize: Int?) {
        /**
         * Extracts sample data from extractor, decodes it and feeds it to audioTrack.
         * Before: Make sure extractor and codec are properly positioned and audioTrack is ready
         * for writing.
         * Parameters:
         *   maxSize = Extract max this number of bytes
         * Returns: Size of last written sample or 0 if there were none.
         */
        if (codec == null) initCodec()

        startCodec()
        return

        var lastSampleSize: Int? = null
        var totalSize = 0
        val timeOutUs: Long = 10000
        val bufferInfo = MediaCodec.BufferInfo()
        var inputEnded = false
        var outputEnded = false

        /**
         * TODO:
         * Can we run codec.start() on init, but make it only load PRIMING_SIZE bytes and then
         * pause? And then on play, resume codec work where we left off?
         * Maybe if we just let CodecCallback.onInputBuffersAvailable() be a no-op when maxSize
         * is reached?
         */

        codec?.also { codec ->
            do {
                if (!inputEnded) {
                    val inputBufferIndex = codec.dequeueInputBuffer(timeOutUs)
                    if (inputBufferIndex >= 0) {
                        codec.getInputBuffer(inputBufferIndex)?.also { buffer ->
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize >= 0) totalSize += sampleSize
                            // Check maxSize against probable totalSize after _next_ iteration:
                            if (sampleSize < 0 || (maxSize != null && totalSize + sampleSize > maxSize)) {
                                codec.queueInputBuffer(
                                        inputBufferIndex, 0, if (sampleSize >= 0) sampleSize else 0,
                                        extractor.sampleTime, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        } ?: Log.d(LOG_TAG, "codec.getInputBuffer($inputBufferIndex) returned null")
                    } else Log.d(LOG_TAG, "dequeueInputBuffer: returned $inputBufferIndex")
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeOutUs)
                when {
                    outputBufferIndex >= 0 -> {
                        codec.getOutputBuffer(outputBufferIndex)?.let { buffer ->
                            audioTrack.write(buffer, bufferInfo.size, AudioTrack.WRITE_BLOCKING)
                            if (bufferInfo.size > 0) lastSampleSize = bufferInfo.size
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputEnded = true
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                        }
                                ?: Log.d(LOG_TAG, "codec.getOutputBuffer($outputBufferIndex) returned null")
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                        Log.d(LOG_TAG, "dequeueOutputBuffer: Output format has changed to ${codec.outputFormat}")
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Log.d(LOG_TAG, "dequeueOutputBuffer: 'try again later' (probably timed out)")
                    }
                    else -> Log.d(LOG_TAG, "dequeueOutputBuffer: returned $outputBufferIndex")
                }
            } while (!outputEnded)

            //codec.flush()
        }
        // return lastSampleSize ?: 0
    }

    override fun run() {
        // TODO: Do this on SoundPlayer thread instead?
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        audioTrack.play()
        extractMode = ExtractMode.REGULAR
        // val lastSampleSize = extract()
        extract()?.also { lastSampleSize ->
            stop(bytesToMilliseconds(lastSampleSize, outputFormat.sampleRate, channelCount))
        }
    }

    fun getMediaFormat(): Pair<Int, MediaFormat>? {
        var format: MediaFormat? = null
        var trackNumber = 0
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            Log.d(LOG_TAG, "getMediaFormat(): name=$name, path=$path, track=$i, mime=$mime, format=$format")
            if (mime?.startsWith("audio/") == true) {
                trackNumber = i
                break
            }
        }
        return format?.let { Pair(trackNumber, format) }
    }

    private fun startCodec() {
        codecCallback.reset()
        codec?.start()
    }


    inner class CodecCallback : MediaCodec.Callback() {
        private var flushAtSampleTime: Long? = null
        private var inputEosReached = false
        private var lastSampleSize: Int? = null
        private var lastSampleTime: Long? = null
        private var onEndOfInputStreamListener: (() -> Unit)? = null
        private var onEndOfOutputStreamListener: ((Int) -> Unit)? = null
        private var onInputSizeLimitReachedListener: (() -> Unit)? = null
        private var totalSize = 0

        private fun onEndOfOutputStream(codec: MediaCodec) {
            codec.flush()
            onEndOfOutputStreamListener?.invoke(lastSampleSize ?: 0)
        }

        fun reset() {
            flushAtSampleTime = null
            inputEosReached = false
            lastSampleSize = null
            lastSampleTime = null
            totalSize = 0
        }

        fun setOnEndOfInputStreamListener(listener: (() -> Unit)?): CodecCallback {
            onEndOfInputStreamListener = listener
            return this
        }

        fun setOnEndOfOutputStreamListener(listener: ((Int) -> Unit)?): CodecCallback {
            onEndOfOutputStreamListener = listener
            return this
        }

        fun setOnInputSizeLimitChangeListener(listener: (() -> Unit)?): CodecCallback {
            onInputSizeLimitReachedListener = listener
            return this
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            /**
             * If we've reached the end of input stream: queue 'end of stream' sample
             * If we are in prime mode and maxSize is reached: flush codec
             */
            if (extractMode == ExtractMode.PRIME && totalSize >= PRIMING_SIZE) {
                // Can we work with input buffer presentation time?
                // When buffer with that time has been processed by output, run codec.flush()?
                // Maybe! It's carried through to onOutputBufferAvailable.info.presentationTimeUs

                // Should this be moved inside getInputBuffer?
                Log.d(LOG_TAG, "onInputBufferAvailable: totalSize[$totalSize] >= PRIMING_SIZE [$PRIMING_SIZE], setting flushAtSampleTime[$flushAtSampleTime] to lastSampleTime[$lastSampleTime]")
                flushAtSampleTime = lastSampleTime
                onInputSizeLimitReachedListener?.invoke()
            } else if (!inputEosReached) {
                codec.getInputBuffer(index)?.also { buffer ->
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    lastSampleTime = extractor.sampleTime
                    codec.queueInputBuffer(
                            index, 0, if (sampleSize >= 0) sampleSize else 0,
                            if (extractor.sampleTime >= 0) extractor.sampleTime else 0,
                            if (sampleSize < 0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    Log.d(LOG_TAG, "codec.getInputBuffer($index): enqueued name=$name, sampleTime=${extractor.sampleTime}, sampleSize=$sampleSize, extractMode=$extractMode")
                    if (sampleSize >= 0) {
                        totalSize += sampleSize
                        extractor.advance()
                    } else {
                        inputEosReached = true
                        onEndOfInputStreamListener?.invoke()
                    }
                }
                        ?: Log.d(LOG_TAG, "codec.getInputBuffer($index) returned null [name=$name, extractMode=$extractMode]")
            }
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            /** Reason for try/catch block: https://developer.android.com/reference/kotlin/android/media/MediaCodec#flush */
            try {
                codec.getOutputBuffer(index)?.let { buffer ->
                    audioTrack.write(buffer, info.size, AudioTrack.WRITE_BLOCKING)
                    codec.releaseOutputBuffer(index, false)
                    Log.d(LOG_TAG, "codec.getOutputBuffer($index): wrote name=$name, presentationTimeUs=${info.presentationTimeUs}, size=${info.size}, extractMode=$extractMode")
                    if (info.size > 0) lastSampleSize = info.size
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(LOG_TAG, "codec.getOutputBuffer($index): EOS reached, invoking onEndOfStreamCallback [name=$name, extractMode=$extractMode]")
                        onEndOfOutputStream(codec)
                    }
                    flushAtSampleTime?.also {
                        if (info.presentationTimeUs >= it) {
                            Log.d(LOG_TAG, "codec.getOutputBuffer($index): flushAtSampleTime[$flushAtSampleTime] reached, flushing codec [name=$name, info.presentationTimeUs=${info.presentationTimeUs}, extractMode=$extractMode]")
                            onEndOfOutputStream(codec)
                        }
                    }
                }
                        ?: Log.d(LOG_TAG, "codec.getOutputBuffer($index) returned null [name=$name, extractMode=$extractMode]")
            } catch (e: IllegalStateException) {
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Log.e(LOG_TAG, "CodecCallback reported error for $codec: $e")
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(LOG_TAG, "CodecCallback: format for $codec changed from $outputFormat to $format")
            val channelMask =
                    when {
                        format.containsKey(MediaFormat.KEY_CHANNEL_MASK) -> format.getInteger(MediaFormat.KEY_CHANNEL_MASK)
                        else -> {
                            when (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                                1 -> AudioFormat.CHANNEL_OUT_MONO
                                2 -> AudioFormat.CHANNEL_OUT_STEREO
                                else -> null
                            }
                        }
                    }
            val encoding =
                    if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                    else null
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            if ((channelMask != null && channelMask != outputFormat.channelMask) ||
                    (encoding != null && encoding != outputFormat.encoding) ||
                    (sampleRate != outputFormat.sampleRate)) {
                outputFormat = AudioFormat.Builder()
                        .setChannelMask(channelMask ?: outputFormat.channelMask)
                        .setEncoding(encoding ?: outputFormat.encoding)
                        .setSampleRate(sampleRate)
                        .build()
                audioTrack = AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setBufferSizeInBytes(BUFFER_SIZE)
                        .setAudioFormat(outputFormat)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
            }
        }

        private fun onInputBufferAvailableOriginal(codec: MediaCodec, index: Int) {
            /** Save in case we need to reverse */
            codec.getInputBuffer(index)?.also { buffer ->
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize >= 0) totalSize += sampleSize

                // Check maxSize against probable totalSize after _next_ iteration:
                if (sampleSize < 0 || (PRIMING_SIZE > 0 && totalSize + sampleSize > PRIMING_SIZE)) {
                    //if (sampleSize < 0 || (maxSize > 0 && totalSize + sampleSize > maxSize)) {
                    codec.queueInputBuffer(
                            index, 0, if (sampleSize >= 0) sampleSize else 0,
                            extractor.sampleTime, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
                    ?: Log.d(LOG_TAG, "codec.getInputBuffer($index) returned null [name=$name, extractMode=$extractMode]")
        }
    }


    enum class State { UNINITIALIZED, INITIALIZING, READY, PLAYING }


    enum class ExtractMode { PRIME, REGULAR }


    companion object {
        // Buffer size: 0.4 sec of 44.1 kHz mono raw audio (or 0.2 sec stereo)
        // Try increasing until it runs good
        const val BUFFER_SIZE = (0.4 * 44100 * 16 / 8).toInt()

        // Priming size: Same as buffer size; try changing around until it runs good
        const val PRIMING_SIZE = BUFFER_SIZE
        const val SAMPLE_RATE = 44100
        const val LOG_TAG = "AudioFile"
    }

}