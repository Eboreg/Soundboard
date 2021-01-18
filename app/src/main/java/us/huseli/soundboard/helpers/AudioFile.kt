@file:Suppress("unused")

package us.huseli.soundboard.helpers

import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer

class AudioFile(
        private val path: String, private val name: String, onInit: ((AudioFile) -> Unit)? = null) {
    /** name is really only for debugging purposes */

    // Public val's & var's
    val duration: Long
    val isPlaying: Boolean
        get() = state == State.PLAYING

    // Private val's to be initialized in init
    private val audioAttributes: AudioAttributes
    private val codecCallback: CodecCallback
    private val inputFormat: MediaFormat
    private val mime: String

    // Private val's initialized here
    private val extractor = MediaExtractor().apply { setDataSource(path) }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    // Private var's to be initialized later on
    private var audioTrack: AudioTrack
    private var channelCount: Int
    private var extractMode = ExtractMode.PRIME
    private var outputFormat: AudioFormat

    // Private var's initialized here
    private var codec: MediaCodec? = null
    private var onErrorListener: ((AudioFile, Error) -> Unit)? = null
    private var onReadyListener: ((AudioFile) -> Unit)? = null
    private var onPlayListener: ((AudioFile) -> Unit)? = null
    private var onStopListener: ((AudioFile) -> Unit)? = null
    private var onWarningListener: ((AudioFile, Error) -> Unit)? = null
    private var queuedStopJob: Job? = null
    private var state = State.INITIALIZING
        set(value) {
            Log.d(LOG_TAG, "state changed from $field to $value")
            field = value
        }

    init {
        val mediaFormatPair = getMediaFormat() ?: throw AudioFileException(Error.GET_MEDIA_TYPE)
        val trackNumber = mediaFormatPair.first
        inputFormat = mediaFormatPair.second
        // InputFormat duration is in MICROseconds!
        duration = (inputFormat.getLong(MediaFormat.KEY_DURATION) / 1000)
        mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw AudioFileException(Error.GET_MIME_TYPE)

        audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        outputFormat = getAudioFormat(inputFormat, null).second
        audioTrack = buildAudioTrack(outputFormat)

        codecCallback = CodecCallback()
        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) initCodec()

        val mbs = AudioTrack.getMinBufferSize(outputFormat.sampleRate, outputFormat.channelMask, outputFormat.encoding)

        extractor.selectTrack(trackNumber)
        prime(onInit)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(LOG_TAG, "init: name=$name, mime=$mime, minBufferSize=$mbs, bufferSizeInFrames=${audioTrack.bufferSizeInFrames}, channelCount=$channelCount, inputFormat=$inputFormat, outputFormat=$outputFormat")
        }
    }

    /********** PUBLIC METHODS (except listener setters) **********/

    fun play() {
        if (state != State.ERROR) {
            val mbs = AudioTrack.getMinBufferSize(outputFormat.sampleRate, outputFormat.channelMask, outputFormat.encoding)
            Log.d(LOG_TAG, "Play name=$name, inputFormat=$inputFormat, outputFormat=$outputFormat, mime=$mime, duration=$duration, audioAttributes=$audioAttributes, audioTrack=$audioTrack, channelCount=$channelCount, min buffer size=$mbs bytes")
            state = State.INITIALIZING
            extractMode = ExtractMode.REGULAR
            audioTrack.play()
            extract()
        }
    }

    fun release() {
        // audioTrackListener.release()
        audioTrack.release()
        codec?.release()
        scope.cancel()
    }

    fun restart() {
        Log.d(LOG_TAG, "restart: init")
        if (state == State.PLAYING) {
            queuedStopJob?.cancel()
            flushCodec(codec)
            audioTrack.pause()
            audioTrack.flush()
            state = State.INITIALIZING
            codecCallback.reset()
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            audioTrack.play()
            codec?.start()
        } else play()
    }

    fun setVolume(value: Int): AudioFile {
        audioTrack.setVolume(value.toFloat() / 100)
        return this
    }

    fun stop() {
        /** Stop immediately */
        if (state == State.PLAYING) {
            state = State.STOPPED
            audioTrack.pause()
            onStopListener?.invoke(this)
            codec?.let {
                Log.d(LOG_TAG, "Flushing codec because playback finished")
                flushCodec(it)
            }
            audioTrack.flush()
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            prime()
        }
    }

    /********** PRIVATE METHODS **********/

    private fun buildAudioTrack(audioFormat: AudioFormat) = buildAudioTrack(audioFormat, AudioManager.AUDIO_SESSION_ID_GENERATE)

    @Throws(AudioFileException::class)
    private fun buildAudioTrack(audioFormat: AudioFormat, sessionId: Int): AudioTrack {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val builder = AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(BUFFER_SIZE * channelCount)
                        .setSessionId(sessionId)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                builder.build()
            } else AudioTrack(audioAttributes, audioFormat, BUFFER_SIZE * channelCount, AudioTrack.MODE_STREAM, sessionId)
        } catch (e: IllegalStateException) {
            throw AudioFileException(Error.BUILD_AUDIO_TRACK)
        }
    }

    private fun bytesToMilliseconds(bytes: Int): Long {
        /** milliseconds = 1000 * bytes / (hz * frameSizeInBytes) */
        // Let's assume we always output 16 bit (== 2 bytes per sample) PCM for simplicity
        return ((1000 * bytes) / (outputFormat.sampleRate * 2 * channelCount)).toLong()
    }

    private fun extract(maxSize: Int? = null) {
        /**
         * Extracts sample data from extractor and feeds it to audioTrack.
         * Before: Make sure extractor is positioned at the correct sample and audioTrack is ready
         * for writing.
         * Parameters:
         *   maxSize = Extract max this number of bytes
         * Returns: Size of last written sample, 0 if there were none, or null if not applicable.
         */
        when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractRaw(maxSize)
            else -> extractEncoded()
        }
    }

    @Throws(AudioFileException::class)
    private fun initCodec() {
        val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(inputFormat)
                ?: throw AudioFileException(Error.NO_SUITABLE_CODEC)
        codec = MediaCodec.createByCodecName(codecName).also {
            it.setCallback(codecCallback)
            it.configure(inputFormat, null, null, 0)
        }
    }

    private fun extractEncoded() {
        try {
            if (codec == null) initCodec()
            codecCallback.reset()
            codec?.start()
        } catch (e: Exception) {
            onError(Error.CODEC_START, e)
        }
    }

    private fun extractRaw(maxSize: Int?) = scope.launch {
        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE * channelCount)
        var totalSize = 0
        var shouldQuit = false
        do {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize >= 0) {
                totalSize += sampleSize
                Log.d(LOG_TAG, "extractRaw: name=$name, writing $sampleSize bytes from $buffer to audioTrack (totalSize=$totalSize)")
                writeAudioTrack(buffer, sampleSize)
                buffer.clear()
                extractor.advance()
            }
            // Check maxSize against probable totalSize after _next_ iteration:
            if (sampleSize < 0 || (maxSize != null && totalSize + sampleSize > maxSize)) {
                Log.d(LOG_TAG, "extractRaw: totalSize=$totalSize, sampleSixe=$sampleSize, maxSize=$maxSize")
                shouldQuit = true
            }
        } while (!shouldQuit)
    }

    private fun flushCodec(codec: MediaCodec?) {
        try {
            codec?.flush()
        } catch (e: IllegalStateException) {
        }
        codecCallback.flushed = true
    }

    private fun framesToMilliseconds(frames: Int): Long {
        // milliseconds = 1000 * frames / hz
        // 1 frame in 16 bit mono = 2 bytes, stereo = 4 bytes
        // Let's assume we always output 16 bit (== 2 bytes per sample) PCM for simplicity
        // TODO: Do we need to factor in stereo here or is that already accounted for?
        return ((1000 * frames) / outputFormat.sampleRate).toLong()
    }

    private fun getAudioFormat(inputFormat: MediaFormat, oldFormat: AudioFormat?): Pair<Boolean, AudioFormat> {
        /** Return: 'has changed' + mediaFormat */
        val inputChannelMask = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_MASK))
            inputFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK) else 0
        val inputChannelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val channelMask =
                when {
                    inputChannelMask > 0 -> inputChannelMask
                    else -> {
                        when (inputChannelCount) {
                            1 -> AudioFormat.CHANNEL_OUT_MONO
                            2 -> AudioFormat.CHANNEL_OUT_STEREO
                            else -> oldFormat?.channelMask
                        }
                    }
                }
        val encoding =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
                    inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                else oldFormat?.encoding

        val hasChanged = ((channelMask != null && channelMask != oldFormat?.channelMask) || (encoding != null && encoding != oldFormat?.encoding))

        val audioFormatBuilder = AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
        if (channelMask != null && channelMask > 0) audioFormatBuilder.setChannelMask(channelMask)
        if (encoding != null) audioFormatBuilder.setEncoding(encoding)
        val audioFormat = audioFormatBuilder.build()
        channelCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioFormat.channelCount else inputChannelCount

        return Pair(hasChanged, audioFormat)
    }

    private fun getMediaFormat(): Pair<Int, MediaFormat>? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return Pair(i, format)
        }
        return null
    }

    private fun onError(errorType: Error, exception: Exception? = null) {
        Log.e(LOG_TAG, "errorType=$errorType, exception=$exception", exception)
        onErrorListener?.invoke(this, errorType)
    }

    private fun onPlayStarted() {
        /** Called when the first block is about to be queued up */
        state = State.PLAYING
        onPlayListener?.invoke(this@AudioFile)
        queuedStopJob = scope.launch { softStop(duration) }
    }

    private fun onWarning(errorType: Error, exception: Exception? = null) {
        Log.w(LOG_TAG, "errorType=$errorType, exception=$exception", exception)
        onWarningListener?.invoke(this, errorType)
    }

    private fun prime(callback: ((AudioFile) -> Unit)? = null) {
        /**
         * Pre-load audioTrack with some reasonable amount of data for low latency.
         * Expects extractor to be ready and positioned at beginning of track.
         */
        extractMode = ExtractMode.PRIME
        if (DO_PRIMING) extract(PRIMING_SIZE)
        //Log.d(LOG_TAG, "prime(): invoking $onReadyListener")
        callback?.invoke(this)
        onReadyListener?.invoke(this)
        state = State.READY
    }

    private fun rebuildAudioTrack(audioFormat: AudioFormat, sessionId: Int): AudioTrack? {
        return try {
            buildAudioTrack(audioFormat, sessionId)
        } catch (e: AudioFileException) {
            onError(Error.BUILD_AUDIO_TRACK, e)
            null
        }
    }

    private suspend fun softStop(delay: Long = 0) {
        /** Await end of stream, then stop */
        Log.d(LOG_TAG, "softStop: Init, delay=$delay, playbackHeadPosition=${audioTrack.playbackHeadPosition} frames, ${framesToMilliseconds(audioTrack.playbackHeadPosition)} milliseconds")
        delay(delay)
        if (audioTrack.playbackHeadPosition <= 0) {
            Log.d(LOG_TAG, "softStop: Waited $delay ms but playhead is still <= 0, stopping anyway")
        } else {
            framesToMilliseconds(audioTrack.playbackHeadPosition).also { ms ->
                if (ms < delay) {
                    Log.d(LOG_TAG, "softStop: Playhead ($ms) still less than duration ($duration), waiting ${delay - ms} more ms, then stopping")
                    delay(delay - ms)
                } else Log.d(LOG_TAG, "softStop: Stopping, playbackHeadPosition=$ms milliseconds")
            }
        }
        stop()
        queuedStopJob = null
    }

    @WorkerThread
    private fun writeAudioTrack(buffer: ByteBuffer, sampleSize: Int) {
        Log.d(LOG_TAG, "writeAudioTrack: name=$name, buffer=$buffer, sampleSize=$sampleSize")
        if (state != State.PLAYING) onPlayStarted()
        audioTrack.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING).also {
            when (it) {
                AudioTrack.ERROR_BAD_VALUE -> onWarning(Error.OUTPUT_BAD_VALUE)
                AudioTrack.ERROR_DEAD_OBJECT -> onWarning(Error.OUTPUT_DEAD_OBJECT)
                AudioTrack.ERROR_INVALID_OPERATION -> onWarning(Error.OUTPUT_NOT_PROPERLY_INITIALIZED)
                AudioTrack.ERROR -> onWarning(Error.OUTPUT)
                else -> Log.d(LOG_TAG, "writeAudioTrack: audioTrack.write() returned $it")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioTrack.underrunCount.takeIf { it > 0 }?.let { Log.d(LOG_TAG, "writeAudioTrack: $it underruns for $name") }
        }
    }


    inner class CodecCallback : MediaCodec.Callback() {
        private var flushAtSampleTime: Long? = null
        private var inputEosReached = false
        private var lastSampleTime: Long? = null
        private var outputBuffer = ByteBuffer.allocate(BUFFER_SIZE * channelCount)
        private var totalSize = 0
        private val writeMutex = Mutex()

        var flushed = false

        fun reset() {
            flushed = false
            flushAtSampleTime = null
            inputEosReached = false
            lastSampleTime = null
            outputBuffer.clear()
            totalSize = 0
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            /**
             * If we've reached the end of input stream: queue 'end of stream' sample
             * If we are in prime mode and maxSize is reached: flush codec
             */
            if (extractMode == ExtractMode.PRIME && totalSize >= PRIMING_SIZE) {
                // TODO: Should this be moved inside getInputBuffer?
                if (flushAtSampleTime == null) {
                    //Log.d(LOG_TAG, "onInputBufferAvailable: totalSize[$totalSize] >= PRIMING_SIZE [$PRIMING_SIZE], setting flushAtSampleTime[$flushAtSampleTime] to lastSampleTime[$lastSampleTime]")
                    flushAtSampleTime = lastSampleTime
                }
            } else if (!inputEosReached) {
                try {
                    val buffer = codec.getInputBuffer(index)
                    if (buffer != null) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        Log.d(LOG_TAG, "onInputBufferAvailable: index=$index, sampleSize=$sampleSize, sampleTime=${extractor.sampleTime}")
                        if (extractor.sampleTime >= 0) lastSampleTime = extractor.sampleTime
                        codec.queueInputBuffer(
                                index, 0, if (sampleSize >= 0) sampleSize else 0,
                                if (extractor.sampleTime >= 0) extractor.sampleTime else 0,
                                if (sampleSize < 0) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        //Log.d(LOG_TAG, "codec.getInputBuffer($index): enqueued name=$name, sampleTime=${extractor.sampleTime}, sampleSize=$sampleSize, extractMode=$extractMode")
                        if (sampleSize >= 0) {
                            totalSize += sampleSize
                            extractor.advance()
                        } else {
                            inputEosReached = true
                        }
                    } // else Log.d(LOG_TAG, "codec.getInputBuffer($index) returned null [name=$name, extractMode=$extractMode]")
                } catch (e: IllegalStateException) {
                    onWarning(Error.CODEC_WRONG_STATE, e)
                } catch (e: MediaCodec.CodecException) {
                    onWarning(Error.CODEC, e)
                }
            }
        }

        private fun write() {
            outputBuffer.limit(outputBuffer.position())
            outputBuffer.rewind()
            writeAudioTrack(outputBuffer, outputBuffer.limit())
            outputBuffer.clear()
        }

        private suspend fun putOutputBuffer(buffer: ByteBuffer, force: Boolean = false) = writeMutex.withLock {
            // private fun putOutputBuffer(buffer: ByteBuffer, force: Boolean = false) {
            // If outputBuffer is full: deal with it first
            if (buffer.remaining() > outputBuffer.remaining()) {
                Log.d(LOG_TAG, "putOutputBuffer: outputBuffer full, writing [outputBuffer=$outputBuffer, buffer=$buffer]")
                write()
            }
            outputBuffer.put(buffer)
            if (force) {
                // Padding with zeros
                while (outputBuffer.remaining() > 0) outputBuffer.put(0)
                Log.d(LOG_TAG, "putOutputBuffer: force=true, writing [outputBuffer=$outputBuffer, buffer=$buffer]")
                write()
            }
        }

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            /** Reason for if statement: https://developer.android.com/reference/kotlin/android/media/MediaCodec#flush */
            if (!flushed) {
                try {
                    val buffer = codec.getOutputBuffer(index)
                    if (buffer != null) {
                        val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val force = flushAtSampleTime?.let { info.presentationTimeUs >= it }
                                ?: false
                        Log.d(LOG_TAG, "onOutputBufferAvailable: index=$index, size=${info.size}, presentationTimeUs=${info.presentationTimeUs}, eos=$eos")
                        scope.launch { putOutputBuffer(buffer, force || eos) }
                        codec.releaseOutputBuffer(index, false)
                        if (force) {
                            Log.d(LOG_TAG, "Flushing codec because priming ready")
                            flushCodec(codec)
                        }
                    } // else Log.d(LOG_TAG, "codec.getOutputBuffer($index) returned null [name=$name, extractMode=$extractMode]")
                } catch (e: Exception) {
                }
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) = onWarning(Error.CODEC, e)

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            val (hasChanged, audioFormat) = getAudioFormat(format, outputFormat)
            Log.d(LOG_TAG, "onOutputFormatChanged: hasChanged=$hasChanged, format=$format, old output format=$outputFormat, new output format=$audioFormat")
            if (hasChanged) {
                outputFormat = audioFormat
                rebuildAudioTrack(audioFormat, audioTrack.audioSessionId)?.let { audioTrack = it }
            }
        }
    }

    class AudioFileException(val errorType: Error, message: String?) : Exception(message) {
        constructor(errorType: Error) : this(errorType, null)

        init {
            Log.e(LOG_TAG, "AudioFile threw $errorType error: ${message ?: "no message"}")
        }
    }


    /********** LISTENER SETTERS **********/
    fun setOnErrorListener(listener: (AudioFile, Error) -> Unit): AudioFile {
        onErrorListener = listener
        return this
    }

    fun setOnPlayListener(listener: ((AudioFile) -> Unit)): AudioFile {
        onPlayListener = listener
        return this
    }

    fun setOnReadyListener(listener: ((AudioFile) -> Unit)): AudioFile {
        onReadyListener = listener
        return this
    }

    fun setOnStopListener(listener: ((AudioFile) -> Unit)): AudioFile {
        onStopListener = listener
        return this
    }

    fun setOnWarningListener(listener: (AudioFile, Error) -> Unit): AudioFile {
        onWarningListener = listener
        return this
    }


    enum class Error {
        GET_MEDIA_TYPE,
        GET_MIME_TYPE,
        BUILD_AUDIO_TRACK,
        CODEC,
        CODEC_GET_WRITE_OUTPUT_BUFFER,
        CODEC_WRONG_STATE,
        NO_SUITABLE_CODEC,
        CODEC_START,
        OUTPUT,
        OUTPUT_BAD_VALUE,
        OUTPUT_DEAD_OBJECT,
        OUTPUT_NOT_PROPERLY_INITIALIZED,
    }

    enum class ExtractMode { PRIME, REGULAR }

    enum class State { READY, PLAYING, INITIALIZING, STOPPED, ERROR }

    companion object {
        // Try messing around until it runs good
        const val BUFFER_SIZE = (1 * 44100 * 16 / 8).toInt()

        // Priming size; try changing around until it runs good
        const val PRIMING_SIZE = BUFFER_SIZE / 4
        const val LOG_TAG = "AudioFile"
        const val AUDIOTRACK_LISTEN_INTERVAL: Long = 50  // milliseconds
        const val SAMPLE_RATE = 44100
        const val DO_PRIMING = false
    }
}