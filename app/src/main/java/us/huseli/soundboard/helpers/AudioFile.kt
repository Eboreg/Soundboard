@file:Suppress("unused")

package us.huseli.soundboard.helpers

import android.media.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.min

@Suppress("RedundantSuspendModifier")
class AudioFile(
        private val path: String,
        private val name: String,
        baseBufferSize: Int,
        onInit: ((AudioFile) -> Unit)? = null) {

    // Public val's & var's
    val duration: Long
    val isPlaying: Boolean
        get() = state == State.PLAYING

    // Private val's to be initialized in init
    private val audioAttributes: AudioAttributes
    private val bufferSize: Int

    //private val codecCallback: CodecCallback
    private val inputMediaFormat: MediaFormat
    private val mime: String

    // Private val's initialized here
    private val extractor = MediaExtractor().apply { setDataSource(path) }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    // Private var's to be initialized later on
    private var audioTrack: AudioTrack
    private var channelCount: Int
    private var extractMode = ExtractMode.PRIME
    private var outputAudioFormat: AudioFormat

    // Private var's initialized here
    private var codec: MediaCodec? = null
    private var extractJob: Job? = null
    private var initCodecJob: Job? = null
    private var onErrorListener: ((AudioFile, Error) -> Unit)? = null
    private var onReadyListener: ((AudioFile) -> Unit)? = null
    private var onPlayListener: ((AudioFile) -> Unit)? = null
    private var onStopListener: ((AudioFile) -> Unit)? = null
    private var onWarningListener: ((AudioFile, Error) -> Unit)? = null
    private var overrunSampleData: ByteBuffer? = null
    private var primedSize = 0
    private var queuedStopJob: Job? = null
    private var state = State.INITIALIZING
        set(value) {
            if (field != value) {
                Log.d(LOG_TAG, "state changed from $field to $value")
                field = value
                @Suppress("NON_EXHAUSTIVE_WHEN")
                when (value) {
                    State.STOPPED -> onStopListener?.invoke(this)
                    State.READY -> onReadyListener?.invoke(this)
                    State.PLAYING -> onPlayListener?.invoke(this)
                }
            }
        }

    init {
        val mediaFormatPair = getMediaFormat() ?: throw AudioFileException(Error.GET_MEDIA_TYPE)
        val trackNumber = mediaFormatPair.first
        inputMediaFormat = mediaFormatPair.second
        // InputFormat duration is in MICROseconds!
        duration = (inputMediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000)
        mime = inputMediaFormat.getString(MediaFormat.KEY_MIME)
                ?: throw AudioFileException(Error.GET_MIME_TYPE)

        audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        channelCount = inputMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        bufferSize = baseBufferSize * channelCount
        outputAudioFormat = getAudioFormat(inputMediaFormat, null).second
        audioTrack = buildAudioTrack(outputAudioFormat)

        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) initCodecJob = scope.launch { initCodec() }

        val mbs = AudioTrack.getMinBufferSize(outputAudioFormat.sampleRate, outputAudioFormat.channelMask, outputAudioFormat.encoding)

        extractor.selectTrack(trackNumber)
        scope.launch { prime(onInit) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(LOG_TAG, "init: name=$name, mime=$mime, minBufferSize=$mbs, bufferSizeInFrames=${audioTrack.bufferSizeInFrames}, channelCount=$channelCount, inputFormat=$inputMediaFormat, outputFormat=$outputAudioFormat")
        }
    }

    /********** PUBLIC METHODS (except listener setters) **********/

    fun play() {
        // Testing:
/*
        scope.launch {
            Log.d(LOG_TAG, "******* play(): start playing")
            audioTrack.play()
            state = State.PLAYING

            Log.d(LOG_TAG, "******* play(): start regular extract")
            extractMode = ExtractMode.REGULAR
            overrunSampleData?.also {
                Log.d(LOG_TAG, "******* play(): overrun data exists: $it")
                writeAudioTrack(it)
                overrunSampleData = null
            }
            extract()

            Log.d(LOG_TAG, "******* play(): delay $duration ms")
            delay(duration)

            Log.d(LOG_TAG, "******* play(): flushing and initing stuff")
            state = State.STOPPED
            extractJob?.cancel()
            queuedStopJob?.cancel()
            audioTrack.pause()
            audioTrack.flush()
            codec?.let {
                flushCodec(it)
            }
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            Log.d(LOG_TAG, "******* play(): done initing, begin priming")
            extractMode = ExtractMode.PRIME
            primedSize = 0
            extract()
            // onReadyListener?.invoke(this@AudioFile)
            state = State.READY
            Log.d(LOG_TAG, "******* play(): priming finished, primedsize=$primedSize, bufferSize=$bufferSize, overrunSampleData=$overrunSampleData")
        }
*/

        if (state != State.ERROR) {
            if (state == State.READY) doPlay()
            else scope.launch { awaitState(State.READY, 1000L) { doPlay() } }
        }
    }

    fun release() {
        // audioTrackListener.release()
        audioTrack.release()
        codec?.release()
        extractor.release()
        scope.cancel()
    }

    fun restart() {
        Log.d(LOG_TAG, "restart: init")
        if (state == State.PLAYING) stop(false)
        play()
    }

    fun setVolume(value: Int): AudioFile {
        audioTrack.setVolume(value.toFloat() / 100)
        return this
    }

    fun stop(prime: Boolean = true) {
        /** Stop immediately */
        if (state == State.PLAYING) {
            state = State.STOPPED
            extractJob?.cancel()
            queuedStopJob?.cancel()
            audioTrack.pause()
            audioTrack.flush()
            flushCodec(codec)
            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            if (prime) scope.launch { prime() }
        }
    }

    /********** PRIVATE METHODS **********/

    private suspend fun awaitState(desiredState: State, timeOutMs: Long, function: () -> Unit) {
        var elapsedMs = 0L
        while (state != desiredState && elapsedMs < timeOutMs) {
            delay(WAIT_INTERVAL)
            elapsedMs += WAIT_INTERVAL
        }
        if (state == desiredState) function.invoke()
        else onWarning(Error.TIMEOUT)
    }

    @Throws(AudioFileException::class)
    private fun buildAudioTrack(audioFormat: AudioFormat): AudioTrack {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val builder = AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                builder.build()
            } else AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
        } catch (e: IllegalStateException) {
            throw AudioFileException(Error.BUILD_AUDIO_TRACK)
        }
    }

    private fun bytesToMilliseconds(bytes: Int): Long {
        /**
         * milliseconds = 1000 * bytes / (hz * frameSizeInBytes)
         * Let's assume we always output 16 bit (== 2 bytes per sample) PCM for simplicity
         */
        return ((1000 * bytes) / (outputAudioFormat.sampleRate * 2 * channelCount)).toLong()
    }

    private fun doPlay() {
        // Log.d(LOG_TAG, "Play name=$name, inputFormat=$inputMediaFormat, outputFormat=$outputAudioFormat, mime=$mime, duration=$duration, audioAttributes=$audioAttributes, audioTrack=$audioTrack, channelCount=$channelCount")
        audioTrack.play()
        state = State.INIT_PLAY
        extractMode = ExtractMode.REGULAR
        extractJob = scope.launch {
            overrunSampleData?.also {
                writeAudioTrack(it)
                overrunSampleData = null
            }
            extract()
        }
    }

    private suspend fun extract() {
        /**
         * Extracts sample data from extractor and feeds it to audioTrack.
         * Before: Make sure extractor is positioned at the correct sample and audioTrack is ready
         * for writing.
         */
        Log.d(LOG_TAG, "******* extract start")
        when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractRaw()
            else -> extractEncoded()
        }
    }

    private suspend fun extractEncoded() {
        /**
         * When priming: Ideally extract exactly `bufferSize` bytes of audio data. If there is some
         * overshoot, put it in `overrunSampleData`. Try to accomplish this by running codec input
         * and output "serially", i.e. only get input buffer when there has been a successful
         * output buffer get just before (except for the first iteration, of course).
         */
        Log.d(LOG_TAG, "******* extractEncoded start")
        if (initCodecJob?.isActive == true) initCodecJob?.join()
        var inputEos = false
        var outputRetries = 0
        var outputStopped = false
        var doExtraction = true
        var totalSize = primedSize
        while (!outputStopped) {
            // yield()  // checks for cancellation
            if (!inputEos && doExtraction) inputEos = processInputBuffer()
            val (outputResult, sampleSize) = processOutputBuffer(totalSize)
            totalSize += sampleSize
            when (outputResult) {
                ProcessOutputResult.SUCCESS -> {
                    outputRetries = 0
                    // We don't _know_ that the next buffer will be of the same size as the current
                    // one, but it's an educated guess that's good enough:
                    if (extractMode == ExtractMode.PRIME) doExtraction = totalSize + sampleSize < bufferSize
                    outputStopped = false
                }
                ProcessOutputResult.EOS -> {
                    if (extractMode == ExtractMode.PRIME) doExtraction = false
                    outputStopped = true
                }
                else -> {
                    if (extractMode == ExtractMode.PRIME) doExtraction = false
                    outputStopped = outputRetries++ >= 5
                }
            }
            Log.d(LOG_TAG, "******* extractEncoded: outputResult=$outputResult, outputRetries=$outputRetries, outputStopped=$outputStopped, doExtraction=$doExtraction, extractMode=$extractMode")
        }
        if (extractMode == ExtractMode.PRIME) primedSize = totalSize
        Log.d(LOG_TAG, "Extract finished, total size=$totalSize, extractMode=$extractMode")
    }

    private suspend fun extractRaw() {
        val buffer = ByteBuffer.allocate(bufferSize)
        var totalSize = primedSize
        var extractorDone = false
        var bufferFull = false
        while (!extractorDone && !bufferFull) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize >= 0) {
                totalSize += sampleSize
                //Log.d(LOG_TAG, "extractRaw: name=$name, writing $sampleSize bytes from $buffer to audioTrack (totalSize=$totalSize)")
                writeAudioTrack(buffer)
                buffer.clear()
                if (extractMode == ExtractMode.PRIME) bufferFull = totalSize + sampleSize > bufferSize
            }
            extractorDone = !extractor.advance()
        }
        if (extractMode == ExtractMode.PRIME) primedSize = totalSize
    }

    private fun flushCodec(codec: MediaCodec?) {
        try {
            codec?.flush()
        } catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "flushCodec error", e)
        }
    }

    private fun framesToMilliseconds(frames: Int): Long {
        // milliseconds = 1000 * frames / hz
        // 1 frame in 16 bit mono = 2 bytes, stereo = 4 bytes
        // Let's assume we always output 16 bit (== 2 bytes per sample) PCM for simplicity
        // TODO: Do we need to factor in stereo here or is that already accounted for?
        return ((1000 * frames) / outputAudioFormat.sampleRate).toLong()
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

    @Throws(AudioFileException::class)
    @Synchronized
    private suspend fun initCodec() {
        val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(inputMediaFormat)
                ?: throw AudioFileException(Error.NO_SUITABLE_CODEC)
        @Suppress("BlockingMethodInNonBlockingContext")
        codec = MediaCodec.createByCodecName(codecName).also {
            it.configure(inputMediaFormat, null, null, 0)
            it.start()
        }
    }

    private fun onError(errorType: Error, exception: Exception? = null) {
        Log.e(LOG_TAG, "errorType=$errorType, exception=$exception", exception)
        onErrorListener?.invoke(this, errorType)
    }

    private fun onPlayStarted() {
        /** Called when the first block is about to be queued up */
        Log.d(LOG_TAG, "onPlayStarted(): starting")
        state = State.PLAYING
        queuedStopJob = scope.launch { softStop(duration) }
        Log.d(LOG_TAG, "onPlayStarted(): finishing, queuedStopJob=$queuedStopJob")
    }

    private fun onWarning(errorType: Error, exception: Exception? = null) {
        Log.w(LOG_TAG, "errorType=$errorType, exception=$exception", exception)
        onWarningListener?.invoke(this, errorType)
    }

    private suspend fun prime(callback: ((AudioFile) -> Unit)? = null) {
        /**
         * Pre-load audioTrack with some reasonable amount of data for low latency.
         * Expects extractor to be ready and positioned at beginning of track.
         */
        extractMode = ExtractMode.PRIME
        primedSize = 0
        if (DO_PRIMING) extract()
        //Log.d(LOG_TAG, "prime(): invoking $onReadyListener")
        callback?.invoke(this)
        state = State.READY
    }

    private suspend fun processInputBuffer(): Boolean {
        /** Return: extractorDone */
        Log.d(LOG_TAG, "******* processInputBuffer start")
        val timeoutUs = 1000L
        var extractorDone = false
        var sampleSize: Int
        var inputEos = false

        return codec?.let { codec ->
            try {
                val index = codec.dequeueInputBuffer(timeoutUs)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)
                    if (buffer != null) {
                        do {
                            // yield()  // checks for cancellation
                            if (extractorDone) {
                                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEos = true
                                break
                            }
                            sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize > 0) {
                                codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                            }
                            extractorDone = !extractor.advance()
                            Log.d(LOG_TAG, "******* processInputBuffer: index=$index, sampleSize=$sampleSize, extractorDone=$extractorDone")
                        } while (sampleSize == 0 || inputEos)
                    }
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error in codec input", e)
            }
            Log.d(LOG_TAG, "******* processInputBuffer: done")
            extractorDone
        } ?: true
    }

    private suspend fun processOutputBuffer(totalSize: Int): Pair<ProcessOutputResult, Int> {
        /** Return: ProcessOutputResult, size written this iteration */
        Log.d(LOG_TAG, "******* processOutputBuffer start")
        val timeoutUs = 1000L
        val info = MediaCodec.BufferInfo()
        var outputEos: Boolean

        codec?.also { codec ->
            try {
                val index = codec.dequeueOutputBuffer(info, timeoutUs)
                // if (index >= 0 && (state == State.PLAYING || state == State.INIT_PLAY)) {
                if (index >= 0) {
                    val buffer = codec.getOutputBuffer(index)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        codec.releaseOutputBuffer(index, false)
                        return Pair(ProcessOutputResult.CODEC_CONFIG, 0)
                    } else if (buffer != null) {
                        // yield()  // checks for cancellation
                        outputEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        Log.d(LOG_TAG, "******* processOutputBuffer: index=$index, buffer=$buffer, outputEos=$outputEos")
                        if (outputEos && totalSize < MINIMUM_SAMPLE_SIZE) {
                            // TODO: Is this necessary?
                            val elementsToAdd = min(MINIMUM_SAMPLE_SIZE - totalSize, buffer.capacity() - buffer.limit())
                            val writableBuffer = ByteBuffer.allocate(buffer.limit() + elementsToAdd)
                            buffer.position(buffer.limit())
                            writableBuffer.put(buffer)
                            for (i in 0 until elementsToAdd) writableBuffer.put(0)
                            writableBuffer.limit(writableBuffer.position())
                            writableBuffer.rewind()
                            writeAudioTrack(writableBuffer)
                        } else
                            writeAudioTrack(buffer)
                        codec.releaseOutputBuffer(index, false)
                        return if (outputEos) Pair(ProcessOutputResult.EOS, info.size) else Pair(ProcessOutputResult.SUCCESS, info.size)
                    } else return Pair(ProcessOutputResult.NO_BUFFER, 0)
                } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val (hasChanged, audioFormat) = getAudioFormat(codec.outputFormat, outputAudioFormat)
                    if (hasChanged) {
                        outputAudioFormat = audioFormat
                        rebuildAudioTrack()
                    }
                    return Pair(ProcessOutputResult.OUTPUT_FORMAT_CHANGED, 0)
                } else return Pair(ProcessOutputResult.NO_BUFFER, 0)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error in codec output", e)
                return Pair(ProcessOutputResult.ERROR, 0)
            }
        }
        return Pair(ProcessOutputResult.NO_CODEC, 0)
    }

    private suspend fun rebuildAudioTrack() {
        try {
            audioTrack.release()
            audioTrack = buildAudioTrack(outputAudioFormat)
        } catch (e: AudioFileException) {
            onError(Error.BUILD_AUDIO_TRACK, e)
        }
    }

    private suspend fun softStop(delay: Long = 0) {
        /** Await end of stream, then stop */
        Log.d(LOG_TAG, "softStop: Init, delay=$delay, playbackHeadPosition=${audioTrack.playbackHeadPosition} frames, ${framesToMilliseconds(audioTrack.playbackHeadPosition)} milliseconds")
        delay(delay)
        if (audioTrack.playbackHeadPosition <= 0) {
            Log.d(LOG_TAG, "softStop: Waited $delay ms but playhead is still <= 0, stopping anyway")
        } else {
            yield()  // checks for cancellation
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

    private fun tryQueueInputBuffer(codec: MediaCodec, index: Int, size: Int, presentationTimeUs: Long, sampleFlags: Int): Boolean {
        return try {
            codec.queueInputBuffer(index, 0, size, presentationTimeUs, sampleFlags)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    private fun writeAudioTrack(buffer: ByteBuffer) {
        /**
         * Will only write to AudioTrack if state is INIT_PLAY or PLAYING (or maybe not?)
         */
        Log.d(LOG_TAG, "writeAudioTrack: name=$name, buffer=$buffer, sampleSize=${buffer.remaining()}, state=$state")
        if (state == State.INIT_PLAY) onPlayStarted()
        val sampleSize = buffer.remaining()

        audioTrack.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING).also {
            when (it) {
                AudioTrack.ERROR_BAD_VALUE -> onWarning(Error.OUTPUT_BAD_VALUE)
                AudioTrack.ERROR_DEAD_OBJECT -> onWarning(Error.OUTPUT_DEAD_OBJECT)
                AudioTrack.ERROR_INVALID_OPERATION -> onWarning(Error.OUTPUT_NOT_PROPERLY_INITIALIZED)
                AudioTrack.ERROR -> onWarning(Error.OUTPUT)
                else -> {
                    Log.d(LOG_TAG, "writeAudioTrack: audioTrack.write() returned $it")
                    if (it < sampleSize) {
                        /**
                         * "Note that upon return, the buffer position (audioData.position()) will
                         * have been advanced to reflect the amount of data that was successfully
                         * written to the AudioTrack."
                         * https://developer.android.com/reference/kotlin/android/media/AudioTrack#write_5
                         */
                        overrunSampleData = ByteBuffer.allocateDirect(sampleSize - it).put(buffer).apply { position(0) }
                    }
                }
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
        TIMEOUT,
    }

    enum class ExtractMode { PRIME, REGULAR }

    enum class State { READY, INIT_PLAY, PLAYING, INITIALIZING, INIT_STOP, STOPPED, ERROR }

    enum class ProcessOutputResult { SUCCESS, OUTPUT_FORMAT_CHANGED, CODEC_CONFIG, NO_BUFFER, EOS, ERROR, NO_CODEC }

    companion object {
        const val LOG_TAG = "AudioFile"
        const val WAIT_INTERVAL: Long = 50  // milliseconds
        const val SAMPLE_RATE = 44100
        const val DO_PRIMING = true
        const val MINIMUM_SAMPLE_SIZE = 75000
        private fun tryGetOutputBuffer(codec: MediaCodec, index: Int): ByteBuffer? {
            return try {
                codec.getOutputBuffer(index)
            } catch (e: Exception) {
                null
            }
        }
    }
}