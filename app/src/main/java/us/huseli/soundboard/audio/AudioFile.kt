package us.huseli.soundboard.audio

import android.media.*
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.min

@Suppress("RedundantSuspendModifier")
class AudioFile(private val sound: Sound, baseBufferSize: Int, stateListener: StateListener? = null) {
    // Public val's & var's
    val duration: Long
    val isPlaying: Boolean
        get() = state == State.PLAYING

    // Private val's to be initialized in init
    private val audioAttributes: AudioAttributes
    private val inputMediaFormat: MediaFormat
    private val mime: String

    // Private val's initialized here
    private val extractor = MediaExtractor()
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private val stateListeners =
        mutableListOf<StateListener>().also { if (stateListener != null) it.add(stateListener) }

    // Private var's to be initialized later on
    private var bufferSize: Int
    private var channelCount: Int
    private var outputAudioFormat: AudioFormat

    // Private var's initialized here
    private var audioTrack: AudioTrack? = null
    private var codec: MediaCodec? = null
    private var extractJob: Job? = null
    private var overrunSampleData: ByteBuffer? = null
    private var primedSize = 0
    private var queuedStopJob: Job? = null

    private var state = State.INITIALIZING
        set(value) {
            if (field != value) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "state changed from $field to $value, this=$this, sound=$sound")
                field = value
                @Suppress("NON_EXHAUSTIVE_WHEN")
                when (value) {
                    State.STOPPED -> stateListeners.forEach { it.onStop(this) }
                    State.READY -> stateListeners.forEach { it.onReady() }
                    State.PLAYING -> stateListeners.forEach { it.onPlay() }
                    State.RELEASED -> stateListeners.forEach { it.onReleased() }
                }
            }
        }

    init {
        inputMediaFormat = initExtractor() ?: throw AudioFileException(Error.GET_MEDIA_TYPE, sound)

        // InputFormat duration is in MICROseconds!
        duration = (inputMediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000)
        mime = inputMediaFormat.getString(MediaFormat.KEY_MIME)
            ?: throw AudioFileException(Error.GET_MIME_TYPE, sound)

        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        channelCount = inputMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        bufferSize = baseBufferSize * channelCount
        outputAudioFormat = getAudioFormat(inputMediaFormat, null).second

        stateListeners.forEach { it.onInit(this) }

        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "init finished: this=$this, sound=$sound, audioTrack=$audioTrack, mime=$mime, channelCount=$channelCount, inputFormat=$inputMediaFormat, outputFormat=$outputAudioFormat, baseBufferSize=$baseBufferSize, bufferSize=$bufferSize")
    }

    /********** PUBLIC METHODS **********/

    fun changeBufferSize(baseBufferSize: Int) {
        if (baseBufferSize * channelCount != bufferSize) {
            bufferSize = baseBufferSize * channelCount
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "changeBufferSize: baseBufferSize=$baseBufferSize, bufferSize=$bufferSize")
            if (state != State.RELEASED && state != State.ERROR) {
                state = State.INITIALIZING
                audioTrack = buildAudioTrack()
                state = State.READY
            }
        }
    }

    fun play() {
        if (state != State.ERROR) {
            scope.launch {
                if (state == State.READY) doPlay()
                else {
                    // Try forcing stop if something got fucked up
                    doStop()
                    prepare()
                    awaitState(State.READY, 1000L) { launch { doPlay() } }
                }
            }
        }
    }

    fun prepare(): AudioFile {
        state = State.INITIALIZING
        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) {
            if (codec == null) codec = initCodec()
            else flushCodec(codec)
        }
        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (audioTrack == null) audioTrack = buildAudioTrack()
        primedSize = 0
        if (overrunSampleData != null && BuildConfig.DEBUG)
            Log.d(LOG_TAG, "**** prepare: overrunSampleData=$overrunSampleData, sound=$sound")
        overrunSampleData = null
        if (DO_PRIMING) scope.launch { prime() }
        state = State.READY
        return this
    }

    fun reinit() {
        initExtractor()
        prepare()
    }

    fun release() {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "release(): sound=$sound, this=$this")
        state = State.RELEASED
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
        }
        extractJob?.cancel()
        queuedStopJob?.cancel()
        audioTrack?.release()
        codec?.release()
        extractor.release()
        audioTrack = null
        codec = null
        scope.cancel()
    }

    fun restart() {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "**** restart: init, sound=$sound")
        stop()
        prepare()
        play()
    }

    fun setVolume(value: Int): AudioFile {
        audioTrack?.setVolume(value.toFloat() / 100)
        return this
    }

    fun stop(): AudioFile {
        /** Used for user-initiated hard stop, cancels any queued future stop job */
        queuedStopJob?.cancel()
        runBlocking { doStop() }
        return this
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

    private fun buildAudioTrack(): AudioTrack? {
        val track = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val builder = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(outputAudioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setPerformanceMode(
                    AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
                )
                builder.build()
            } else AudioTrack(
                audioAttributes,
                outputAudioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } catch (e: Exception) {
            onError(Error.BUILD_AUDIO_TRACK, e)
            null
        }
        track?.setVolume(sound.volume.toFloat() / 100)
        return track
    }

    // @Synchronized
    private suspend fun doPlay() {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doPlay: playing sound=$sound, audioTrack=$audioTrack, this=$this")
        audioTrack?.play()
        state = State.INIT_PLAY
        extractJob?.cancelAndJoin()
        extractJob = scope.launch {
            overrunSampleData?.also {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                    "**** doPlay: writing overrunSampleData=$overrunSampleData, state=$state, sound=$sound")
                writeAudioTrack(it)
                overrunSampleData = null
            }
            extract()
        }
    }

    private suspend fun doStop() {
        /** Stop immediately. force = don't care about present state */
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doStop: cancelling extractJob, sound=$sound, state=$state")
        extractJob?.cancelAndJoin()
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doStop: extractJob cancelled, sound=$sound, state=$state")
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
        }
        state = State.STOPPED
        audioTrack?.flush()
    }

    // @Synchronized
    private suspend fun extract(priming: Boolean = false) {
        /**
         * Extracts sample data from extractor and feeds it to audioTrack.
         * Before: Make sure extractor is positioned at the correct sample and audioTrack is ready
         * for writing.
         */
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG, "**** Begin extract(), state=$state, primedSize=$primedSize, sound=$sound")
        when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractRaw()
            else -> codec?.also { extractEncoded(it, priming) }
        }
    }

    private suspend fun extractEncoded(codec: MediaCodec, priming: Boolean) {
        /**
         * When priming: Ideally extract exactly `bufferSize` bytes of audio data. If there is some
         * overshoot, put it in `overrunSampleData`. Try to accomplish this by running codec input
         * and output "serially", i.e. only get input buffer when there has been a successful
         * output buffer get just before (except for the first iteration, of course).
         */
        val job = coroutineContext[Job]

        var stop = false
        var inputResult = ProcessInputResult.CONTINUE
        var totalSize = primedSize
        var outputRetries = 0
        var doExtraction = true

        while (!stop && job?.isActive == true) {
            if (inputResult != ProcessInputResult.END && doExtraction)
                inputResult = processInputBuffer(codec, inputResult)
            val (outputResult, sampleSize) = processOutputBuffer(codec, totalSize)
            totalSize += sampleSize
            when (outputResult) {
                ProcessOutputResult.SUCCESS -> {
                    outputRetries = 0
                    // We don't _know_ that the next buffer will be of the same size as the current
                    // one, but it's an educated guess that's good enough:
                    if (priming) doExtraction = totalSize + sampleSize < bufferSize
                    stop = false
                }
                ProcessOutputResult.EOS -> {
                    if (priming) doExtraction = false
                    stop = true
                }
                else -> {
                    if (priming) doExtraction = false
                    stop = outputRetries++ >= 5
                }
            }
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "extractEncoded: inputResult=$inputResult, outputResult=$outputResult, outputRetries=$outputRetries, stop=$stop, doExtraction=$doExtraction, state=$state, priming=$priming, sound=$sound")
        }
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "extractEncoded: finished, stop=$stop, isActive=${job?.isActive}")
    }


    private suspend fun extractRaw() {
        val buffer = ByteBuffer.allocate(bufferSize)
        val job = coroutineContext[Job]

        var totalSize = primedSize
        var extractorDone = false
        var bufferFull = false
        while (!extractorDone && !bufferFull && job?.isActive == true) {
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
            if (BuildConfig.DEBUG) Log.e(LOG_TAG, "flushCodec error, sound=$sound", e)
        }
    }

    private fun framesToMilliseconds(frames: Int): Long {
        // milliseconds = 1000 * frames / hz
        // 1 frame in 16 bit mono = 2 bytes, stereo = 4 bytes
        // Let's assume we always output 16 bit (== 2 bytes per sample) PCM for simplicity
        // TODO: Do we need to factor in stereo here or is that already accounted for?
        return ((1000 * frames) / outputAudioFormat.sampleRate).toLong()
    }

    private fun getAudioFormat(
        inputFormat: MediaFormat,
        oldFormat: AudioFormat?
    ): Pair<Boolean, AudioFormat> {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && inputFormat.containsKey(
                    MediaFormat.KEY_PCM_ENCODING
                )
            )
                inputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
            else oldFormat?.encoding

        val hasChanged =
            ((channelMask != null && channelMask != oldFormat?.channelMask) || (encoding != null && encoding != oldFormat?.encoding))

        val audioFormatBuilder =
            AudioFormat.Builder().setSampleRate(inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        if (channelMask != null && channelMask > 0) audioFormatBuilder.setChannelMask(channelMask)
        if (encoding != null) audioFormatBuilder.setEncoding(encoding)
        val audioFormat = audioFormatBuilder.build()
        channelCount =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) audioFormat.channelCount else inputChannelCount

        return Pair(hasChanged, audioFormat)
    }

    private fun initCodec(): MediaCodec? {
        val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(inputMediaFormat)
        return if (codecName != null) {
            MediaCodec.createByCodecName(codecName).also {
                it.configure(inputMediaFormat, null, null, 0)
                it.start()
            }
        } else {
            onError(Error.NO_SUITABLE_CODEC)
            null
        }
    }

    private fun initExtractor(): MediaFormat? {
        extractor.setDataSource(sound.path)
        for (trackNumber in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackNumber)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                extractor.selectTrack(trackNumber)
                return format
            }
        }
        return null
    }

    @Suppress("SameParameterValue")
    private fun onError(errorType: Error, exception: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(LOG_TAG, "errorType=$errorType, sound=$sound", exception)
        stateListeners.forEach { it.onError(errorType) }
    }

    private fun onPlayStarted() {
        /** Called when the first block is about to be queued up */
        state = State.PLAYING
        softStop(duration)
    }

    private fun onWarning(errorType: Error, exception: Exception? = null) {
        if (BuildConfig.DEBUG) Log.w(LOG_TAG, "errorType=$errorType, exception=$exception, sound=$sound", exception)
        stateListeners.forEach { it.onWarning(errorType) }
    }

    private suspend fun prime() {
        /**
         * Pre-load audioTrack with some reasonable amount of data for low latency.
         * Expects extractor to be ready and positioned at beginning of track.
         */
        state = State.PRIMING
        extractJob?.cancelAndJoin()
        extractJob = scope.launch { extract(true) }
    }

    private suspend fun processInputBuffer(codec: MediaCodec, previousResult: ProcessInputResult): ProcessInputResult {
        /** Return: extractorDone */
        val job = coroutineContext[Job]
        val timeoutUs = 1000L
        var extractorDone = false
        var sampleSize: Int

        try {
            val index = codec.dequeueInputBuffer(timeoutUs)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index)
                if (buffer != null) {
                    do {
                        if (previousResult == ProcessInputResult.END_NEXT) {
                            codec.queueInputBuffer(
                                index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            break
                        }
                        sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            codec.queueInputBuffer(
                                index, 0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                        }
                        extractorDone = !extractor.advance()
                        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                            "processInputBuffer: index=$index, sampleSize=$sampleSize, extractorDone=$extractorDone, state=$state, sound=$sound")
                    } while (sampleSize == 0 && job?.isActive == true)
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(LOG_TAG, "Error in codec input, sound=$sound", e)
        }
        return when {
            previousResult == ProcessInputResult.END_NEXT -> ProcessInputResult.END
            extractorDone -> ProcessInputResult.END_NEXT
            else -> ProcessInputResult.CONTINUE
        }
    }

    private fun processOutputBuffer(codec: MediaCodec, totalSize: Int): Pair<ProcessOutputResult, Int> {
        /** Return: ProcessOutputResult, size written this iteration */
        val timeoutUs = 1000L
        val info = MediaCodec.BufferInfo()

        try {
            val index = codec.dequeueOutputBuffer(info, timeoutUs)
            when {
                index >= 0 -> {
                    val buffer = codec.getOutputBuffer(index)
                    when {
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 -> {
                            codec.releaseOutputBuffer(index, false)
                            return Pair(ProcessOutputResult.CODEC_CONFIG, 0)
                        }
                        buffer != null -> {
                            val outputEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                                "processOutputBuffer: index=$index, buffer=$buffer, outputEos=$outputEos, state=$state, sound=$sound")
                            val outputBuffer = if (outputEos && totalSize < MINIMUM_SAMPLE_SIZE) {
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
                                writableBuffer
                            } else buffer
                            val writtenSize = writeAudioTrack(outputBuffer)
                            codec.releaseOutputBuffer(index, false)
                            return Pair(if (outputEos) ProcessOutputResult.EOS else ProcessOutputResult.SUCCESS,
                                writtenSize)
                        }
                        else -> return Pair(ProcessOutputResult.NO_BUFFER, 0)
                    }
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val (hasChanged, audioFormat) = getAudioFormat(
                        codec.outputFormat,
                        outputAudioFormat
                    )
                    if (hasChanged) {
                        outputAudioFormat = audioFormat
                        rebuildAudioTrack()
                    }
                    return Pair(ProcessOutputResult.OUTPUT_FORMAT_CHANGED, 0)
                }
                else -> return Pair(ProcessOutputResult.NO_BUFFER, 0)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(LOG_TAG, "Error in codec output, sound=$sound", e)
            return Pair(ProcessOutputResult.ERROR, 0)
        }
    }

    private fun rebuildAudioTrack() {
        try {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "rebuildAudioTrack: releasing audioTrack=$audioTrack, sound=$sound")
            audioTrack?.release()
            audioTrack = buildAudioTrack()
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "rebuildAudioTrack: built new audioTrack=$audioTrack, sound=$sound")
        } catch (e: AudioFileException) {
            onError(Error.BUILD_AUDIO_TRACK, e)
        }
    }

    private fun softStop(delay: Long = 0) {
        if (queuedStopJob?.isActive == true) return  // there can only be one
        queuedStopJob = scope.launch {
            /** Await end of stream, then stop */
            delay(delay)
            if (audioTrack?.playbackHeadPosition ?: 0 <= 0) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                    "softStop: Waited $delay ms but playhead is still <= 0, stopping anyway; audioTrack=$audioTrack, sound=$sound")
            } else {
                framesToMilliseconds(audioTrack?.playbackHeadPosition ?: 0).also { ms ->
                    if (ms < delay) {
                        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                            "softStop: Playhead ($ms) still less than duration ($duration), waiting ${delay - ms} more ms, then stopping, sound=$sound")
                        delay(delay - ms)
                    } else if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                        "softStop: Stopping, playbackHeadPosition=$ms milliseconds, sound=$sound")
                }
            }
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "softStop: running doStop()")
            doStop()
        }
    }

    private fun writeAudioTrack(buffer: ByteBuffer): Int {
        /**
         * Will only write to AudioTrack if state is INIT_PLAY, PRIMING, or PLAYING
         * Returns number of bytes written
         */
        if (state == State.INIT_PLAY || state == State.PLAYING || state == State.PRIMING) {
            val sampleSize = buffer.remaining()
            if (state == State.INIT_PLAY) onPlayStarted()

            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "writeAudioTrack: writing to audioTrack=$audioTrack, sound=$sound")
            audioTrack?.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING)?.also {
                when (it) {
                    AudioTrack.ERROR_BAD_VALUE -> onWarning(Error.OUTPUT_BAD_VALUE)
                    AudioTrack.ERROR_DEAD_OBJECT -> onWarning(Error.OUTPUT_DEAD_OBJECT)
                    AudioTrack.ERROR_INVALID_OPERATION -> onWarning(Error.OUTPUT_NOT_PROPERLY_INITIALIZED)
                    AudioTrack.ERROR -> onWarning(Error.OUTPUT)
                    else -> {
                        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                            "writeAudioTrack: wrote $it bytes, buffer=$buffer, state=$state, sampleSize=$sampleSize, overshoot=${sampleSize - it}, sound=$sound")
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
                        return it
                    }
                }
            }
        }
        return 0
    }


    class AudioFileException(
        val errorType: Error,
        override val cause: Throwable? = null,
        message: String? = null,
        sound: Sound? = null
    ) : Exception(message, cause) {
        constructor(errorType: Error, sound: Sound) : this(errorType, null, null, sound)

        init {
            if (BuildConfig.DEBUG) Log.e(LOG_TAG,
                "AudioFile threw $errorType error: ${message ?: "no message"}, sound=$sound")
        }
    }


    interface StateListener {
        fun onError(errorType: Error)
        fun onInit(audioFile: AudioFile)
        fun onPlay()
        fun onReady()
        fun onReleased()
        fun onStop(audioFile: AudioFile)
        fun onWarning(errorType: Error)
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
    enum class State { INITIALIZING, READY, INIT_PLAY, PLAYING, STOPPED, PRIMING, ERROR, RELEASED }

    enum class ProcessInputResult { CONTINUE, END_NEXT, END }

    enum class ProcessOutputResult { SUCCESS, OUTPUT_FORMAT_CHANGED, CODEC_CONFIG, NO_BUFFER, EOS, ERROR, }

    companion object {
        const val LOG_TAG = "AudioFile"
        const val WAIT_INTERVAL: Long = 50  // milliseconds
        const val DO_PRIMING = false
        const val MINIMUM_SAMPLE_SIZE = 75000
    }
}