@file:Suppress("unused")

package us.huseli.soundboard.audio

import android.media.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound
import java.nio.ByteBuffer
import kotlin.math.min

@Suppress("RedundantSuspendModifier")
class AudioFile(
        sound: Sound,
        baseBufferSize: Int,
        private val isTemporary: Boolean = false,
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
    private val extractor = MediaExtractor().apply { setDataSource(sound.path) }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    // Private var's to be initialized later on
    private var audioTrack: AudioTrack
    private var channelCount: Int
    private var outputAudioFormat: AudioFormat

    // Private var's initialized here
    private var extractJob: Job? = null
    private var initCodecJob: Job? = null
    private var primingExtractJob: Job? = null
    private var queuedStopJob: Job? = null

    private var codec: MediaCodec? = null
    private var overrunSampleData: ByteBuffer? = null
    private var primedSize = 0

    private var onErrorListener: ((AudioFile, Error) -> Unit)? = null
    private var onReadyListener: ((AudioFile) -> Unit)? = null
    private var onPlayListener: ((AudioFile) -> Unit)? = null
    private var onStopListener: ((AudioFile) -> Unit)? = null
    private var onWarningListener: ((AudioFile, Error) -> Unit)? = null

    private var state = State.INITIALIZING
        set(value) {
            if (field != value) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "state changed from $field to $value")
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
        prime(onInit)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (BuildConfig.DEBUG) Log.d(
                LOG_TAG,
                "init: sound=$sound, path=${sound.path}, mime=$mime, minBufferSize=$mbs, bufferSizeInFrames=${audioTrack.bufferSizeInFrames}, channelCount=$channelCount, inputFormat=$inputMediaFormat, outputFormat=$outputAudioFormat"
            )
        }
    }

    /********** PUBLIC METHODS (except listener setters) **********/

    fun play() {
        if (state != State.ERROR) {
            if (state == State.READY) doPlay()
            else scope.launch { awaitState(State.READY, 1000L) { doPlay() } }
        }
    }

    fun release() {
        try {
            audioTrack.pause()
        } catch (e: IllegalStateException) {
        }
        audioTrack.release()
        codec?.release()
        extractor.release()
        scope.cancel()
    }

    fun restart() {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "**** restart: init")
        if (state == State.PLAYING) stop(false)
        play()
    }

    fun setVolume(value: Int): AudioFile {
        audioTrack.setVolume(value.toFloat() / 100)
        return this
    }

    fun stop(doPriming: Boolean = true) {
        /** Stop immediately */
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG,
            "**** stop() called, doPriming=$doPriming, state=$state, isTemporary=$isTemporary"
        )
        if (isTemporary) {
            state = State.STOPPED
            release()
        } else if (state == State.PLAYING) {
            state = State.STOPPED
            extractJob?.cancel()
            queuedStopJob?.cancel()
            audioTrack.pause()
            if (!isTemporary) {
                audioTrack.flush()
                flushCodec(codec)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }
            if (doPriming) prime()
            else state = State.READY
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
        audioTrack.play()
        state = State.INIT_PLAY
        extractJob = scope.launch {
            overrunSampleData?.also {
                if (BuildConfig.DEBUG) Log.d(
                    LOG_TAG,
                    "**** doPlay: writing overrunSampleData=$overrunSampleData, state=$state"
                )
                writeAudioTrack(it)
                overrunSampleData = null
            }
            extract()
        }
    }

    @Synchronized
    private suspend fun extract() {
        /**
         * Extracts sample data from extractor and feeds it to audioTrack.
         * Before: Make sure extractor is positioned at the correct sample and audioTrack is ready
         * for writing.
         */
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG,
            "**** Begin extract(), state=$state, primedSize=$primedSize"
        )
        when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractRaw()
            else -> extractEncoded(state == State.PRIMING)
        }
    }

    private suspend fun extractEncoded(priming: Boolean) {
        /**
         * When priming: Ideally extract exactly `bufferSize` bytes of audio data. If there is some
         * overshoot, put it in `overrunSampleData`. Try to accomplish this by running codec input
         * and output "serially", i.e. only get input buffer when there has been a successful
         * output buffer get just before (except for the first iteration, of course).
         */
        if (initCodecJob?.isActive == true) initCodecJob?.join()
        var inputEos = false
        var outputRetries = 0
        var outputStopped = false
        var doExtraction = true
        var totalSize = primedSize
        val job = if (priming) primingExtractJob else extractJob

        while (!outputStopped && job?.isActive == true) {
            if (!inputEos && doExtraction) inputEos = processInputBuffer(job)
            val (outputResult, sampleSize) = processOutputBuffer(job, totalSize)
            totalSize += sampleSize
            when (outputResult) {
                ProcessOutputResult.SUCCESS -> {
                    outputRetries = 0
                    // We don't _know_ that the next buffer will be of the same size as the current
                    // one, but it's an educated guess that's good enough:
                    if (priming) doExtraction = totalSize + sampleSize < bufferSize
                    outputStopped = false
                }
                ProcessOutputResult.EOS -> {
                    if (priming) doExtraction = false
                    outputStopped = true
                }
                else -> {
                    if (priming) doExtraction = false
                    outputStopped = outputRetries++ >= 5
                }
            }
            if (BuildConfig.DEBUG) Log.d(
                LOG_TAG,
                "extractEncoded: outputResult=$outputResult, outputRetries=$outputRetries, outputStopped=$outputStopped, doExtraction=$doExtraction, state=$state, priming=$priming"
            )
        }
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG,
            "**** extractEncoded: Extract finished: totalSize=$totalSize, totalSize before extract=$primedSize, added=${totalSize - primedSize}, state=$state, priming=$priming"
        )
        if (priming) primedSize = totalSize
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
                writeAudioTrack(buffer)
                buffer.clear()
                if (state == State.PRIMING) bufferFull = totalSize + sampleSize > bufferSize
            }
            extractorDone = !extractor.advance()
        }
        if (state == State.PRIMING) primedSize = totalSize
    }

    private fun flushCodec(codec: MediaCodec?) {
        try {
            codec?.flush()
        } catch (e: IllegalStateException) {
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "flushCodec error", e)
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

        val audioFormatBuilder = AudioFormat.Builder().setSampleRate(inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
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

    @Suppress("SameParameterValue")
    private fun onError(errorType: Error, exception: Exception? = null) {
        if (BuildConfig.DEBUG) Log.e(
            LOG_TAG,
            "errorType=$errorType, exception=$exception",
            exception
        )
        onErrorListener?.invoke(this, errorType)
    }

    private fun onPlayStarted() {
        /** Called when the first block is about to be queued up */
        state = State.PLAYING
        queuedStopJob = scope.launch { softStop(duration) }
    }

    private fun onWarning(errorType: Error, exception: Exception? = null) {
        if (BuildConfig.DEBUG) Log.w(
            LOG_TAG,
            "errorType=$errorType, exception=$exception",
            exception
        )
        onWarningListener?.invoke(this, errorType)
    }

    private fun prime(callback: ((AudioFile) -> Unit)? = null) {
        /**
         * Pre-load audioTrack with some reasonable amount of data for low latency.
         * Expects extractor to be ready and positioned at beginning of track.
         */
        primedSize = 0
        if (overrunSampleData != null) if (BuildConfig.DEBUG) Log.d(
            LOG_TAG,
            "**** prime: overrunSampleData=$overrunSampleData"
        )
        overrunSampleData = null
        if (DO_PRIMING) {
            state = State.PRIMING
            primingExtractJob = scope.launch {
                extract()
                callback?.invoke(this@AudioFile)
                state = State.READY
            }
        } else state = State.READY
    }

    private suspend fun processInputBuffer(job: Job?): Boolean {
        /** Return: extractorDone */
        val timeoutUs = 1000L
        var extractorDone = false
        var sampleSize: Int
        var inputEos = false

        return codec?.let { codec ->
            try {
                val index = codec.dequeueInputBuffer(timeoutUs)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)
                    if (buffer != null && job?.isActive == true) {
                        do {
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
                            if (BuildConfig.DEBUG) Log.d(
                                LOG_TAG,
                                "processInputBuffer: index=$index, sampleSize=$sampleSize, extractorDone=$extractorDone, job=$job, state=$state"
                            )
                        } while (job.isActive && (sampleSize == 0 || inputEos))
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(LOG_TAG, "Error in codec input", e)
            }
            extractorDone
        } ?: true
    }

    private suspend fun processOutputBuffer(job: Job?, totalSize: Int): Pair<ProcessOutputResult, Int> {
        /** Return: ProcessOutputResult, size written this iteration */
        val timeoutUs = 1000L
        val info = MediaCodec.BufferInfo()
        var outputEos: Boolean

        codec?.also { codec ->
            try {
                val index = codec.dequeueOutputBuffer(info, timeoutUs)
                if (index >= 0 && job?.isActive == true) {
                    val buffer = codec.getOutputBuffer(index)
                    @Suppress("CascadeIf")
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        codec.releaseOutputBuffer(index, false)
                        return Pair(ProcessOutputResult.CODEC_CONFIG, 0)
                    } else if (buffer != null) {
                        outputEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        if (BuildConfig.DEBUG) Log.d(
                            LOG_TAG,
                            "processOutputBuffer: index=$index, buffer=$buffer, outputEos=$outputEos, state=$state, job=$job"
                        )
                        if (outputEos && totalSize < MINIMUM_SAMPLE_SIZE) {
                            // TODO: Is this necessary?
                            val elementsToAdd = min(
                                MINIMUM_SAMPLE_SIZE - totalSize,
                                buffer.capacity() - buffer.limit()
                            )
                            val writableBuffer = ByteBuffer.allocate(buffer.limit() + elementsToAdd)
                            buffer.position(buffer.limit())
                            writableBuffer.put(buffer)
                            for (i in 0 until elementsToAdd) writableBuffer.put(0)
                            writableBuffer.limit(writableBuffer.position())
                            writableBuffer.rewind()
                            if (job.isActive) writeAudioTrack(writableBuffer)
                        } else
                            if (job.isActive) writeAudioTrack(buffer)
                        if (job.isActive) codec.releaseOutputBuffer(index, false)
                        return if (outputEos) Pair(ProcessOutputResult.EOS, info.size) else Pair(
                            ProcessOutputResult.SUCCESS, info.size
                        )
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
                if (BuildConfig.DEBUG) Log.w(LOG_TAG, "Error in codec output", e)
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
        delay(delay)
        if (audioTrack.playbackHeadPosition <= 0) {
            if (BuildConfig.DEBUG) Log.d(
                LOG_TAG,
                "softStop: Waited $delay ms but playhead is still <= 0, stopping anyway"
            )
        } else {
            if (queuedStopJob?.isActive != true) return
            framesToMilliseconds(audioTrack.playbackHeadPosition).also { ms ->
                if (ms < delay) {
                    if (BuildConfig.DEBUG) Log.d(
                        LOG_TAG,
                        "softStop: Playhead ($ms) still less than duration ($duration), waiting ${delay - ms} more ms, then stopping"
                    )
                    delay(delay - ms)
                } else if (BuildConfig.DEBUG) Log.d(
                    LOG_TAG,
                    "softStop: Stopping, playbackHeadPosition=$ms milliseconds"
                )
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
         * Will only write to AudioTrack if state is INIT_PLAY, PRIMING, or PLAYING
         */
        if (state == State.INIT_PLAY || state == State.PLAYING || state == State.PRIMING) {
            val sampleSize = buffer.remaining()
            if (state == State.INIT_PLAY) onPlayStarted()

            audioTrack.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING).also {
                when (it) {
                    AudioTrack.ERROR_BAD_VALUE -> onWarning(Error.OUTPUT_BAD_VALUE)
                    AudioTrack.ERROR_DEAD_OBJECT -> onWarning(Error.OUTPUT_DEAD_OBJECT)
                    AudioTrack.ERROR_INVALID_OPERATION -> onWarning(Error.OUTPUT_NOT_PROPERLY_INITIALIZED)
                    AudioTrack.ERROR -> onWarning(Error.OUTPUT)
                    else -> {
                        if (BuildConfig.DEBUG) Log.d(
                            LOG_TAG,
                            "writeAudioTrack: wrote $it bytes, buffer=$buffer, state=$state, sampleSize=$sampleSize, overshoot=${sampleSize - it}"
                        )
                        if (it < sampleSize) {
                            /**
                             * "Note that upon return, the buffer position (audioData.position()) will
                             * have been advanced to reflect the amount of data that was successfully
                             * written to the AudioTrack."
                             * https://developer.android.com/reference/kotlin/android/media/AudioTrack#write_5
                             */
                            overrunSampleData =
                                ByteBuffer.allocateDirect(sampleSize - it).put(buffer)
                                    .apply { position(0) }
                        }
                    }
                }
            }
        }
    }


    class AudioFileException(val errorType: Error, message: String?) : Exception(message) {
        constructor(errorType: Error) : this(errorType, null)

        init {
            if (BuildConfig.DEBUG) Log.e(
                LOG_TAG,
                "AudioFile threw $errorType error: ${message ?: "no message"}"
            )
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

    /**
     * INITIALIZING = initial state, will change to READY when all is initialized
     * READY = only now we can begin playing. Also, onReadyListener will run once
     * INIT_PLAY = state between READY and PLAYING. onPlayStarted() will run once, and thereafter
     *      change state to PLAYING
     * PLAYING = sound is playing, onPlayListener will run once
     * STOPPED = state between PLAYING and READY. onStopListener will run once
     */
    enum class State { INITIALIZING, READY, INIT_PLAY, PLAYING, STOPPED, PRIMING, ERROR }

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