package us.huseli.soundboard.audio

import android.media.*
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound
import java.nio.ByteBuffer
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/**
 * Design decision: `state` is only set in the public methods, including callbacks defined by them.
 * Exception: onError() sets state = State.ERROR.
 *
 * Valid state should also only be checked by the public methods.
 * Exceptions:
 *  - extractEncoded() and extractRaw() check State.INIT_PLAY, to be able to time on-playing-callback as accurately
 *    as possible
 */

class AudioFile(private val sound: Sound, baseBufferSize: Int, listener: Listener? = null) {
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
        mutableListOf<Listener>().also { if (listener != null) it.add(listener) }

    // Private var's to be initialized later on
    private var bufferSize: Int
    private var channelCount: Int
    private var outputAudioFormat: AudioFormat

    // Private var's initialized here
    private var audioTrack: AudioTrack? = null
    private var codec: MediaCodec? = null
    private var extractJob: Job? = null
    private var primedData: ByteBuffer? = null
    private var queuedStopJob: Job? = null

    private var state = State.CREATED
        set(value) {
            // We will not change from ERROR to anything else, because ERROR is final
            if (field != value && field != State.ERROR) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "state changed from $field to $value, this=$this, sound=$sound")
                field = value
                stateListeners.forEach { it.onAudioFileStateChange(value, this) }
            }
        }

    init {
        inputMediaFormat = initExtractor() ?: run {
            onError("Could not get media type")
            throw AudioFileException("Could not get media type", sound)
        }

        // InputFormat duration is in MICROseconds!
        duration = (inputMediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000)
        mime = inputMediaFormat.getString(MediaFormat.KEY_MIME) ?: run {
            onError("Could not get MIME type")
            throw AudioFileException("Could not get MIME type", sound)
        }

        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        channelCount = inputMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        bufferSize = baseBufferSize * channelCount
        outputAudioFormat = getAudioFormat(inputMediaFormat, null).second

        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "init finished: this=$this, sound=$sound, mime=$mime, channelCount=$channelCount, inputFormat=$inputMediaFormat, outputFormat=$outputAudioFormat, baseBufferSize=$baseBufferSize, bufferSize=$bufferSize")
    }

    /********** PUBLIC METHODS **********/

    fun changeBufferSize(baseBufferSize: Int) {
        if (baseBufferSize * channelCount != bufferSize) {
            bufferSize = baseBufferSize * channelCount
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "changeBufferSize: baseBufferSize=$baseBufferSize, bufferSize=$bufferSize")
        }
    }

    fun play() {
        if (state != State.READY)
            onWarning("Play: Illegal state", "play: illegal state $state, should be READY")
        else {
            state = State.INIT_PLAY
            doPlay {
                state = State.PLAYING
                enqueueStop(duration) { state = State.STOPPED }
            }
        }
    }

    fun playAndPrepare() {
        if (state != State.READY)
            onWarning("Play: Illegal state", "playAndPrepare: illegal state $state, should be READY")
        else {
            state = State.INIT_PLAY
            doPlay {
                state = State.PLAYING
                enqueueStop(duration) {
                    doPrepare()
                    doPrime()
                    state = State.READY
                }
            }
        }
    }

    fun prepare(): AudioFile {
        if (!listOf(State.CREATED, State.STOPPED, State.RELEASED).contains(state))
            onWarning("Prepare: Illegal state", "prepare: illegal state $state, should be CREATED, STOPPED, RELEASED")
        else {
            state = State.INITIALIZING
            doPrepare()
            state = State.READY
        }
        return this
    }

    fun prepareAndPrime(): AudioFile {
        if (!listOf(State.CREATED, State.STOPPED, State.RELEASED).contains(state))
            onWarning("Prepare: Illegal state",
                "prepareAndPrime: illegal state $state, should be CREATED, STOPPED, RELEASED")
        else {
            state = State.INITIALIZING
            doPrepare()
            doPrime()
            state = State.READY
        }
        return this
    }

    fun release(): AudioFile {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "release(): sound=$sound, this=$this")
        if (state != State.RELEASED) {
            state = State.RELEASED
            try {
                audioTrack?.pause()
            } catch (e: Exception) {
            }
            extractJob?.cancel()
            queuedStopJob?.cancel()
            audioTrack?.release()
            codec?.release()
            audioTrack = null
            codec = null
            primedData = null
            scope.cancel()
        }
        return this
    }

    suspend fun restart(): AudioFile {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "**** restart: init, sound=$sound")
        if (!listOf(State.PLAYING, State.INIT_PLAY, State.READY).contains(state))
            onWarning("Restart: Illegal state", "restart: illegal state $state, should be PLAYING, INIT_PLAY, or READY")
        else {
            if (state == State.PLAYING || state == State.INIT_PLAY) {
                state = State.INIT_PLAY
                audioTrack?.pause()
                audioTrack?.flush()
                extractJob?.cancelAndJoin()
                doPrepare()
            } else state = State.INIT_PLAY
            doPlay {
                state = State.PLAYING
                enqueueStop(duration) { state = State.STOPPED }
            }
        }
        return this
    }

    suspend fun stop(): AudioFile {
        /**
         * Used for user-initiated hard stop, cancels any queued future stop job
         * It's up to the caller to run prepare() or whatever needs to be done afterwards!
         */
        if (state == State.PLAYING || state == State.INIT_PLAY) {
            queuedStopJob?.cancelAndJoin()
            doStop { state = State.STOPPED }
        } else onWarning("Stop: Illegal state", "stop: illegal state $state, should be PLAYING or INIT_PLAY")
        return this
    }

    suspend fun stopAndPrepare(): AudioFile {
        if (state == State.PLAYING || state == State.INIT_PLAY) {
            queuedStopJob?.cancelAndJoin()
            doStop {
                doPrepare()
                doPrime()
                state = State.READY
            }
        } else onWarning("Stop: Illegal state", "stop: illegal state $state, should be PLAYING or INIT_PLAY")
        return this
    }


    /********** PRIVATE METHODS **********/

    private fun buildAudioTrack(): AudioTrack {
        // val track = audioTrackProvider.acquire(sound, inputMediaFormat)
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
        track.setVolume(sound.volume.toFloat() / 100)
        return track
    }

    private fun checkNotOnMainThread(caller: String) {
        if (Looper.getMainLooper().thread == Thread.currentThread())
            Log.e(LOG_TAG, "checkNotOnMainThread: $caller was called from main thread, but it shouldn't be!")
    }

    private fun doPlay(onPlayStartCallback: (() -> Unit)? = null) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doPlay: playing sound=$sound, audioTrack=$audioTrack, this=$this")
        try {
            // if (mime != MediaFormat.MIMETYPE_AUDIO_RAW && codec == null) codec = initCodec()
            audioTrack = buildAudioTrack()
            audioTrack?.play()
            primedData?.also {
                writeAudioTrack(it)
                primedData = null
            } ?: Log.w(LOG_TAG, "doPlay: primedData is empty! Maybe this is on purpose?")
            extractJob = scope.launch {
                extract(onPlayStartCallback)
            }
        } catch (e: IllegalStateException) {
            onError("Error outputting audio")
        }
    }

    private fun doPrepare() {
        if (BuildConfig.DEBUG) checkNotOnMainThread("doPrepare")

        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) {
            if (codec == null) codec = initCodec()
            else flushCodec(codec)
        }
    }

    private fun doPrime() {
        if (BuildConfig.DEBUG) checkNotOnMainThread("doPrime")

        primedData = ByteBuffer.allocateDirect(bufferSize).also { primedData ->
            if (mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
                // Source: Raw audio
                do {
                    val sampleSize = extractor.readSampleData(primedData, 0)
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doPrime: sampleSize=$sampleSize, state=$state, sound=$sound")
                } while (sampleSize == 0)
            } else codec?.also { codec ->
                // Source: Encoded audio
                var inputResult = ProcessInputResult.CONTINUE
                var outputRetries = 0
                var stop = false
                var totalSize = 0

                while (!stop) {
                    if (inputResult != ProcessInputResult.END)
                        inputResult = processInputBuffer(codec, inputResult)
                    val (outputResult, outputBuffer) = processOutputBuffer(codec)
                    stop = when (outputResult) {
                        ProcessOutputResult.SUCCESS -> {
                            val sampleSize = if (outputBuffer != null) {
                                primedData.put(outputBuffer)
                                outputBuffer.position()
                            } else 0
                            totalSize += sampleSize
                            outputRetries = 0
                            // Stop if another buffer of the same size as this one would overflow primedData
                            totalSize + sampleSize > bufferSize
                        }
                        ProcessOutputResult.EOS -> true
                        else -> outputRetries++ >= 5
                    }
                }
                primedData.limit(primedData.position()).rewind()
                if (BuildConfig.DEBUG) {
                    val logString =
                        "doPrime: totalSize=$totalSize, primedData=$primedData, bufferSize=$bufferSize, state=$state, sound=$sound"
                    if (totalSize == 0) Log.w(LOG_TAG, logString)
                    else Log.d(LOG_TAG, logString)
                }
            }
        }
    }

    private suspend fun doStop(onStoppedCallback: (() -> Unit)? = null) {
        /** Stop immediately */
        if (BuildConfig.DEBUG) checkNotOnMainThread("doStop")
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doStop: cancelling extractJob, sound=$sound, state=$state")
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
        }
        extractJob?.cancelAndJoin()
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "doStop: extractJob cancelled, sound=$sound, state=$state")
        // state = State.STOPPED
        audioTrack?.release()
        audioTrack = null
        onStoppedCallback?.invoke()
        // prepare()
    }

    private fun enqueueStop(delay: Long = 0, onStoppedCallback: (() -> Unit)? = null) {
        if (queuedStopJob?.isActive == true) return  // there can only be one
        queuedStopJob = scope.launch {
            /** Await end of stream, then stop */
            delay(delay)
            if (audioTrack?.playbackHeadPosition ?: 0 <= 0) {
                Log.w(LOG_TAG,
                    "enqueueStop: Waited $delay ms but playhead is still <= 0, stopping anyway; audioTrack=$audioTrack, sound=$sound")
            } else {
                framesToMilliseconds(audioTrack?.playbackHeadPosition ?: 0).also { ms ->
                    if (ms < delay) {
                        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                            "enqueueStop: Playhead ($ms) still less than duration ($duration), waiting ${delay - ms} more ms, then stopping, sound=$sound")
                        delay(delay - ms)
                    } else if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                        "enqueueStop: Stopping, playbackHeadPosition=$ms milliseconds, sound=$sound")
                }
            }
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "enqueueStop: running doStop()")
            doStop(onStoppedCallback)
        }
    }

    private suspend fun extract(onPlayStartCallback: (() -> Unit)? = null) {
        /**
         * Extracts sample data from extractor and feeds it to audioTrack.
         * Before: Make sure extractor is positioned at the correct sample and audioTrack is ready
         * for writing.
         */
        if (BuildConfig.DEBUG) checkNotOnMainThread("extract")
        if (BuildConfig.DEBUG) Log.d(
            LOG_TAG, "**** Begin extract(), state=$state, sound=$sound")

        when (mime) {
            MediaFormat.MIMETYPE_AUDIO_RAW -> extractRaw(onPlayStartCallback)
            else -> codec?.also { extractEncoded(it, onPlayStartCallback) }
        }
    }

    private suspend fun extractEncoded(codec: MediaCodec, onPlayStartCallback: (() -> Unit)? = null) {
        /** Only called by extract() */
        val job = coroutineContext[Job]

        var stop = false
        var inputResult = ProcessInputResult.CONTINUE
        var totalSize = 0
        var outputRetries = 0

        while (!stop && job?.isActive == true) {
            if (inputResult != ProcessInputResult.END)
                inputResult = processInputBuffer(codec, inputResult)
            val (outputResult, outputBuffer) = processOutputBuffer(codec, totalSize)
            if (outputResult == ProcessOutputResult.SUCCESS && outputBuffer != null) {
                totalSize += outputBuffer.remaining()
                writeAudioTrack(outputBuffer)
                if (state == State.INIT_PLAY) onPlayStartCallback?.invoke()
                outputRetries = 0
            }
            stop = when (outputResult) {
                ProcessOutputResult.SUCCESS -> false
                ProcessOutputResult.EOS -> true
                else -> outputRetries++ >= 5
            }
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "extractEncoded: inputResult=$inputResult, outputResult=$outputResult, outputRetries=$outputRetries, stop=$stop, state=$state, sound=$sound")
        }
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "extractEncoded: finished, stop=$stop, isActive=${job?.isActive}")
    }


    private suspend fun extractRaw(onPlayStartCallback: (() -> Unit)? = null) {
        /** Only called by extract() */
        val buffer = ByteBuffer.allocate(bufferSize)
        val job = coroutineContext[Job]

        var totalSize = 0
        var extractorDone = false
        while (!extractorDone && job?.isActive == true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize >= 0) {
                totalSize += sampleSize
                if (writeAudioTrack(buffer) > 0 && state == State.INIT_PLAY) onPlayStartCallback?.invoke()
                buffer.clear()
            }
            extractorDone = !extractor.advance()
        }
    }

    private fun flushCodec(codec: MediaCodec?) {
        try {
            codec?.flush()
        } catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "flushCodec error, sound=$sound", e)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && inputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
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
            try {
                MediaCodec.createByCodecName(codecName).also {
                    it.configure(inputMediaFormat, null, null, 0)
                    it.start()
                }
            } catch (e: Exception) {
                onError("Codec error")
                null
            }
        } else {
            onError("Could not find a suitable codec")
            null
        }
    }

    private fun initExtractor(): MediaFormat? {
        try {
            extractor.setDataSource(sound.path)
            for (trackNumber in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(trackNumber)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    extractor.selectTrack(trackNumber)
                    return format
                }
            }
        } catch (e: Exception) {
            onError("Error initializing sound", e)
        }
        return null
    }

    private fun onError(message: String, exception: Throwable? = null) {
        state = State.ERROR
        Log.e(LOG_TAG, "message=$message, sound=$sound", exception)
        stateListeners.forEach { it.onAudioFileError(message) }
    }

    private fun onWarning(message: String, verboseMessage: String? = null, exception: Exception? = null) {
        Log.w(LOG_TAG,
            "message=${verboseMessage ?: message}, exception=$exception, sound=$sound", exception)
        stateListeners.forEach { it.onAudioFileWarning(message) }
    }

    private fun processInputBuffer(codec: MediaCodec, previousResult: ProcessInputResult): ProcessInputResult {
        if (BuildConfig.DEBUG) checkNotOnMainThread("processInputBuffer")

        val timeoutUs = 1000L
        var extractorDone = false

        try {
            val index = codec.dequeueInputBuffer(timeoutUs)
            if (index >= 0) {
                val buffer = codec.getInputBuffer(index)
                if (buffer != null) {
                    if (previousResult == ProcessInputResult.END_NEXT) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    } else {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            codec.queueInputBuffer(
                                index, 0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                        }
                        extractorDone = !extractor.advance()
                    }
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                        "processInputBuffer: index=$index, extractorDone=$extractorDone, state=$state, sound=$sound")
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in codec input, sound=$sound", e)
        }
        return when {
            previousResult == ProcessInputResult.END_NEXT -> ProcessInputResult.END
            extractorDone -> ProcessInputResult.END_NEXT
            else -> ProcessInputResult.CONTINUE
        }
    }

    private fun processOutputBuffer(codec: MediaCodec, totalSize: Int? = null): Pair<ProcessOutputResult, ByteBuffer?> {
        /** Return: ProcessOutputResult, buffer */
        if (BuildConfig.DEBUG) checkNotOnMainThread("processOutputBuffer")

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
                            return Pair(ProcessOutputResult.CODEC_CONFIG, null)
                        }
                        buffer != null -> {
                            val outputEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                                "processOutputBuffer: index=$index, buffer=$buffer, outputEos=$outputEos, state=$state, sound=$sound")
                            val outputBuffer = if (outputEos && totalSize != null && totalSize < MINIMUM_SAMPLE_SIZE) {
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
                            // TODO: Will it work if I release output buffer before writing buffer to AudioTrack?
                            // val writtenSize = writeAudioTrack(outputBuffer)
                            codec.releaseOutputBuffer(index, false)
                            return Pair(
                                if (outputEos) ProcessOutputResult.EOS else ProcessOutputResult.SUCCESS, outputBuffer)
                        }
                        else -> return Pair(ProcessOutputResult.NO_BUFFER, null)
                    }
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val (hasChanged, audioFormat) = getAudioFormat(codec.outputFormat, outputAudioFormat)
                    if (hasChanged) {
                        outputAudioFormat = audioFormat
                        rebuildAudioTrack()
                    }
                    return Pair(ProcessOutputResult.OUTPUT_FORMAT_CHANGED, null)
                }
                else -> return Pair(ProcessOutputResult.NO_BUFFER, null)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in codec output, sound=$sound", e)
            return Pair(ProcessOutputResult.ERROR, null)
        }
    }

    private fun rebuildAudioTrack() {
        try {
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "rebuildAudioTrack: releasing audioTrack=$audioTrack, sound=$sound")
            audioTrack?.release()
            audioTrack = buildAudioTrack()
            if (BuildConfig.DEBUG) Log.d(LOG_TAG, "rebuildAudioTrack: built new audioTrack=$audioTrack, sound=$sound")
        } catch (e: AudioFileException) {
            onError("Error building audio track")
        }
    }

    private fun writeAudioTrack(buffer: ByteBuffer): Int {
        /**
         * Will only write to AudioTrack if state is INIT_PLAY, PRIMING, or PLAYING
         * Returns number of bytes written
         */
        val sampleSize = buffer.remaining()

        if (BuildConfig.DEBUG) Log.d(LOG_TAG,
            "writeAudioTrack: writing to audioTrack=$audioTrack, sound=$sound, buffer=$buffer, sampleSize=$sampleSize")
        audioTrack?.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING)?.also {
            when (it) {
                AudioTrack.ERROR_BAD_VALUE -> onWarning("Audio output: bad value")
                AudioTrack.ERROR_DEAD_OBJECT -> onWarning("Audio output: dead object")
                AudioTrack.ERROR_INVALID_OPERATION -> onWarning("Audio output: not properly initialized")
                AudioTrack.ERROR -> onWarning("Error outputting audio")
                else -> {
                    if (BuildConfig.DEBUG) {
                        when (val overshoot = sampleSize - it) {
                            0 -> Log.d(LOG_TAG,
                                "writeAudioTrack: wrote $it bytes, buffer=$buffer, state=$state, sampleSize=$sampleSize, sound=$sound")
                            else -> Log.w(LOG_TAG,
                                "writeAudioTrack: wrote $it bytes, buffer=$buffer, overshoot=$overshoot, state=$state, sampleSize=$sampleSize, sound=$sound")
                        }
                    }
                    return it
                }
            }
        }
        return 0
    }


    /********** INNER CLASSES/INTERFACES/ENUMS **********/

    class AudioFileException(
        override val message: String,
        override val cause: Throwable? = null,
        sound: Sound? = null
    ) : Exception(message, cause) {
        constructor(message: String, sound: Sound) : this(message, null, sound)

        init {
            Log.e(LOG_TAG, "AudioFile threw error: $message, sound=$sound", cause)
        }
    }


    interface Listener {
        fun onAudioFileStateChange(state: State, audioFile: AudioFile, message: String? = null)
        fun onAudioFileError(message: String)
        fun onAudioFileWarning(message: String)
    }


    /**
     * CREATED
     *    Means: Is newly created, prepare() has not been run but needs to
     *    Begins: On creation
     *    Ends: When prepare() runs
     *    May change from: Nothing
     *    Changes to: INITIALIZING, RELEASED, ERROR
     * INITIALIZING
     *    Means: Sound is initializing
     *    Begins: On prepare()
     *    Ends: When all is initialized
     *    May change from: CREATED, STOPPED, RELEASED
     *    Changes to: READY or ERROR
     * READY
     *    Means: Everything is ready for playback
     *    Begins: When prepare() has finished
     *    Ends: When playback starts, release() is run or error occurs
     *    May change from: INITIALIZING
     *    Changes to: INIT_PLAY, ERROR or RELEASED
     * INIT_PLAY
     *    Means: User has pressed 'play', playback is initializing
     *    Begins: Directly on playback initialization
     *    Ends: When sound output has begun
     *    May change from: READY
     *    Changes to: PLAYING or ERROR
     * PLAYING
     *    Means: Sound is currently playing
     *    Begins: On first sound output
     *    Ends: On stop
     *    May change from: INIT_PLAY
     *    Changes to: STOPPED or ERROR
     * STOPPED
     *    Means: Playback has stopped and is not (yet) in initializing/ready state
     *    Begins: When user pressed stop or sound has played to the end
     *    Ends: Only when prepare() or release() is run
     *    May change from: PLAYING, INIT_PLAY
     * RELEASED
     *    Means: Everything has been (or is being) released
     *    Begins: As soon as release() is run
     *    Ends: Only when reinit() is run
     *    May change from: All except ERROR
     * ERROR
     *    Means: Any non-recoverable error has occurred
     *    Begins: Well, duh
     *    Ends: Never -- this is final
     *    May change from: Any
     */
    enum class State { CREATED, INITIALIZING, READY, INIT_PLAY, PLAYING, STOPPED, ERROR, RELEASED }

    enum class ProcessInputResult { CONTINUE, END_NEXT, END }

    enum class ProcessOutputResult { SUCCESS, OUTPUT_FORMAT_CHANGED, CODEC_CONFIG, NO_BUFFER, EOS, ERROR, }

    companion object {
        const val LOG_TAG = "AudioFile"
        const val MINIMUM_SAMPLE_SIZE = 75000
    }
}