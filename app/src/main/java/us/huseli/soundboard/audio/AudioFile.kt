package us.huseli.soundboard.audio

import android.media.*
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Constants
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

class AudioFile(private val sound: Sound,
                var volume: Int,
                baseBufferSize: Int,
                private var effect: AudioEffect?,
                private var effectSendLevel: Float,
                listener: Listener? = null) {
    constructor(sound: Sound, baseBufferSize: Int, effect: AudioEffect?, effectSendLevel: Float, listener: Listener?) :
            this(sound, sound.volume, baseBufferSize, effect, effectSendLevel, listener)

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
    private val overflowBuffers = mutableListOf<ByteBuffer>()
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var stateListener = listener

    // Private var's to be initialized later on
    private var bufferSize: Int
    private var channelCount: Int
    private var outputAudioFormat: AudioFormat

    // Private var's initialized here
    private var audioTrack: AudioTrack? = null
    private var codec: MediaCodec? = null
    private var extractJob: Job? = null
    private var extractorDone = false
    private var primedData: ByteBuffer? = null
    private var queuedStopJob: Job? = null

    private var state = State.CREATED
        set(value) {
            // We will not change from ERROR to anything else, because ERROR is final
            if (field != value && field != State.ERROR) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "state changed from $field to $value, this=$this, sound=$sound")
                field = value
                stateListener?.onAudioFileStateChange(value, this)
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
    }

    /********** PUBLIC METHODS **********/

    fun changeBufferSize(baseBufferSize: Int): AudioFile {
        if (baseBufferSize * channelCount != bufferSize) {
            bufferSize = baseBufferSize * channelCount
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "changeBufferSize: baseBufferSize=$baseBufferSize, bufferSize=$bufferSize")
        }
        return this
    }

    suspend fun pause(): AudioFile {
        if (state != State.PLAYING)
            onWarning("Pause: Illegal state", "pause: illegal state $state, should be PLAYING")
        else {
            audioTrack?.pause()
            extractJob?.cancelAndJoin()
            queuedStopJob?.cancelAndJoin()
            state = State.PAUSED
        }
        return this
    }

    fun play(timeoutUs: Long? = null): AudioFile {
        if (state != State.READY)
            onWarning("Play: Illegal state", "play: illegal state $state, should be READY")
        else {
            state = State.INIT_PLAY
            val onTimeoutCallback = {
                state = State.READY
                onWarning("Timeout")
            }
            doPlay(timeoutUs, onTimeoutCallback) {
                state = State.PLAYING
                enqueueStop(duration) { state = State.STOPPED }
            }
        }
        return this
    }

    fun playAndPrepare(): AudioFile {
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
        return this
    }

    fun prepare(): AudioFile {
        if (!listOf(State.CREATED, State.STOPPED, State.RELEASED, State.PAUSED).contains(state))
            onWarning(
                "Prepare: Illegal state",
                "prepare: illegal state $state, should be CREATED, STOPPED, RELEASED, PAUSED")
        else {
            state = State.INITIALIZING
            doPrepare()
            state = State.READY
        }
        return this
    }

    fun prepareAndPrime(): AudioFile {
        if (!listOf(State.CREATED, State.STOPPED, State.RELEASED, State.PAUSED).contains(state))
            onWarning("Prepare: Illegal state",
                "prepareAndPrime: illegal state $state, should be CREATED, STOPPED, RELEASED, PAUSED")
        else {
            state = State.INITIALIZING
            doPrepare()
            doPrime()
            state = State.READY
        }
        return this
    }

    fun release(): AudioFile {
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
            stateListener = null
            audioTrack = null
            codec = null
            primedData = null
            scope.cancel()
        }
        return this
    }

    suspend fun restartAndPrepare(timeoutUs: Long): AudioFile {
        if (!listOf(State.PLAYING, State.INIT_PLAY, State.READY).contains(state))
            onWarning("Restart: Illegal state", "restart: illegal state $state, should be PLAYING, INIT_PLAY, or READY")
        else {
            if (state == State.PLAYING || state == State.INIT_PLAY) {
                state = State.INIT_PLAY
                audioTrack?.pause()
                audioTrack?.flush()
                queuedStopJob?.cancelAndJoin()
                extractJob?.cancelAndJoin()
                doPrepare()
            } else state = State.INIT_PLAY
            val onTimeoutCallback = {
                state = State.READY
                onWarning("Timeout")
            }
            doPlay(timeoutUs, onTimeoutCallback) {
                state = State.PLAYING
                enqueueStop(duration) {
                    doPrepare()
                    doPrime()
                    state = State.READY
                }
            }
        }
        return this
    }

    fun resumeAndPrepare(): AudioFile {
        if (state != State.PAUSED)
            onWarning("Resume: Illegal state", "resume: illegal state $state, should be PAUSED")
        else {
            audioTrack?.play()
            state = State.PLAYING
            overflowBuffers.forEach { writeAudioTrack(it) }
            overflowBuffers.clear()
            extractJob = scope.launch { extract() }
            enqueueStop(duration - framesToMilliseconds(audioTrack?.playbackHeadPosition ?: 0)) {
                doPrepare()
                doPrime()
                state = State.READY
            }
        }
        return this
    }

    fun setEffect(effect: AudioEffect?, sendLevel: Float?) {
        state = State.INITIALIZING
        scope.launch {
            doSetEffect(effect, sendLevel) {
                state = State.READY
            }
        }
    }

    suspend fun stop(): AudioFile {
        /**
         * Used for user-initiated hard stop, cancels any queued future stop job
         * It's up to the caller to run prepare() or whatever needs to be done afterwards!
         */
        if (state == State.PLAYING || state == State.INIT_PLAY) {
            queuedStopJob?.cancelAndJoin()
            doStop()
            state = State.STOPPED
        } else onWarning("Stop: Illegal state", "stop: illegal state $state, should be PLAYING or INIT_PLAY")
        return this
    }

    suspend fun stopAndPrepare(): AudioFile {
        if (state == State.PLAYING || state == State.INIT_PLAY) {
            queuedStopJob?.cancelAndJoin()
            doStop()
            doPrepare()
            doPrime()
            state = State.READY
        } else onWarning("Stop: Illegal state", "stop: illegal state $state, should be PLAYING or INIT_PLAY")
        return this
    }

    fun unsetEffect() = setEffect(null, null)


    /********** PRIVATE METHODS **********/

    private fun buildAudioTrack(): AudioTrack {
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
        track.setVolume(volume.toFloat() / 100)
        // TODO: Remove after testing
        // effect = PresetReverb(100, track.audioSessionId).also { effect ->
        effect?.let {
            track.attachAuxEffect(it.id)
            track.setAuxEffectSendLevel(effectSendLevel)
        }
        return track
    }

    private fun checkNotOnMainThread(caller: String) {
        if (Looper.getMainLooper().thread == Thread.currentThread())
            Log.e(LOG_TAG, "checkNotOnMainThread: $caller was called from main thread, but it shouldn't be!")
    }

    private suspend fun doSetEffect(effect: AudioEffect?, sendLevel: Float?, onReadyCallback: (() -> Unit)? = null) {
        if (BuildConfig.DEBUG) checkNotOnMainThread("doSetEffect")

        this.effect = effect
        if (sendLevel != null) effectSendLevel = sendLevel
        doStop()
        doPrepare()
        doPrime()
        onReadyCallback?.invoke()
    }

    private fun doPlay(onPlayStartCallback: (() -> Unit)? = null) = doPlay(null, null, onPlayStartCallback)

    private fun doPlay(timeoutUs: Long?,
                       onTimeoutCallback: (() -> Unit)? = null,
                       onPlayStartCallback: (() -> Unit)? = null) {
        var callbackDone = false
        try {
            audioTrack = buildAudioTrack()
        } catch (e: Exception) {
            onError("Error building audio track", e)
        }
        try {
            if (timeoutUs != null && System.nanoTime() > timeoutUs) onTimeoutCallback?.invoke()
            else {
                audioTrack?.play()
                primedData?.also {
                    if (writeAudioTrack(it) > 0 && state == State.INIT_PLAY && !callbackDone) {
                        onPlayStartCallback?.invoke()
                        callbackDone = true
                    }
                    primedData = null
                } ?: Log.w(LOG_TAG, "doPlay: primedData is empty! Maybe this is on purpose?")
                extractJob = scope.launch { extract(if (!callbackDone) onPlayStartCallback else null) }
            }
        } catch (e: IllegalStateException) {
            onError("Error outputting audio", e)
        }
    }

    private fun doPrepare() {
        if (BuildConfig.DEBUG) checkNotOnMainThread("doPrepare")

        extractorDone = false
        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) {
            if (codec == null) codec = initCodec()
            else flushCodec(codec)
        }
    }

    private fun doPrime() {
        if (BuildConfig.DEBUG) checkNotOnMainThread("doPrime")

        var totalSize = 0
        extractorDone = false

        primedData = ByteBuffer.allocateDirect(bufferSize).also { primedData ->
            if (mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
                // Source: Raw audio
                do {
                    totalSize = extractor.readSampleData(primedData, 0)
                    extractorDone = !extractor.advance()
                } while (totalSize == 0 && !extractorDone)
            } else codec?.also { codec ->
                // Source: Encoded audio
                var inputResult = ProcessInputResult.CONTINUE
                var outputRetries = 0
                var stop = false

                while (!stop) {
                    if (inputResult != ProcessInputResult.END)
                        inputResult = processInputBuffer(codec, inputResult)
                    val (outputResult, outputBuffer, index) = processOutputBuffer(codec)
                    stop = when (outputResult) {
                        ProcessOutputResult.SUCCESS -> {
                            val sampleSize: Int
                            if (outputBuffer != null) {
                                primedData.put(outputBuffer)
                                sampleSize = outputBuffer.position()
                                if (index != null) codec.releaseOutputBuffer(index, false)
                            } else sampleSize = 0
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
            }
            if (BuildConfig.DEBUG) {
                val logString =
                    "doPrime: totalSize=$totalSize, primedData=$primedData, bufferSize=$bufferSize, state=$state, sound=$sound"
                if (totalSize == 0) Log.w(LOG_TAG, logString)
            }
        }
    }

    private suspend fun doStop(onStoppedCallback: (() -> Unit)? = null) {
        /** Stop immediately */
        if (BuildConfig.DEBUG) checkNotOnMainThread("doStop")
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
        }
        extractJob?.cancelAndJoin()
        //effect?.release()
        audioTrack?.release()
        audioTrack = null
        onStoppedCallback?.invoke()
        overflowBuffers.clear()
    }

    private fun enqueueStop(delay: Long = 0, onStoppedCallback: (() -> Unit)? = null) {
        if (queuedStopJob?.isActive == true) {
            if (BuildConfig.DEBUG) Log.w(LOG_TAG,
                "enqueueStop: Tried to create new queuedStopJob when one is already active")
            return  // there can only be one
        }
        queuedStopJob = scope.launch {
            /** Await end of stream, then stop */
            delay(delay)
            // If playback head position is still 0, just give up
            if (audioTrack?.playbackHeadPosition ?: 0 > 0) {
                framesToMilliseconds(audioTrack?.playbackHeadPosition ?: 0).also { ms ->
                    if (ms < delay) delay(delay - ms)
                }
            }
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
        var callbackDone = false

        while (!stop && job?.isActive == true) {
            if (inputResult != ProcessInputResult.END)
                inputResult = processInputBuffer(codec, inputResult)

            val (outputResult, outputBuffer, index) = processOutputBuffer(codec, totalSize)

            if (outputResult == ProcessOutputResult.OUTPUT_FORMAT_CHANGED)
                audioTrack = rebuildAudioTrack()
            else if (outputResult == ProcessOutputResult.SUCCESS && outputBuffer != null) {
                totalSize += outputBuffer.remaining()
                writeAudioTrack(outputBuffer)
                if (index != null) codec.releaseOutputBuffer(index, false)
                if (state == State.INIT_PLAY && job.isActive && !callbackDone) {
                    onPlayStartCallback?.invoke()
                    callbackDone = true
                }
                outputRetries = 0
            }

            stop = when (outputResult) {
                ProcessOutputResult.SUCCESS -> false
                ProcessOutputResult.EOS -> true
                else -> outputRetries++ >= 5
            }
        }
    }


    private suspend fun extractRaw(onPlayStartCallback: (() -> Unit)? = null) {
        /** Only called by extract() */
        val buffer = ByteBuffer.allocate(bufferSize)
        val job = coroutineContext[Job]
        var callbackDone = false

        var totalSize = 0
        while (!extractorDone && job?.isActive == true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize >= 0) {
                totalSize += sampleSize
                if (writeAudioTrack(buffer) > 0 && state == State.INIT_PLAY && !callbackDone) {
                    onPlayStartCallback?.invoke()
                    callbackDone = true
                }
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
            extractorDone = false
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
        stateListener?.onAudioFileError(message)
    }

    private fun onWarning(message: String, verboseMessage: String? = null, exception: Exception? = null) {
        Log.w(LOG_TAG,
            "message=${verboseMessage ?: message}, exception=$exception, sound=$sound", exception)
        stateListener?.onAudioFileWarning(message)
    }

    private fun processInputBuffer(codec: MediaCodec, previousResult: ProcessInputResult): ProcessInputResult {
        if (BuildConfig.DEBUG) checkNotOnMainThread("processInputBuffer")

        try {
            val index = codec.dequeueInputBuffer(1000L)
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

    private fun processOutputBuffer(codec: MediaCodec, totalSize: Int? = null):
            Triple<ProcessOutputResult, ByteBuffer?, Int?> {
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
                            return Triple(ProcessOutputResult.CODEC_CONFIG, null, null)
                        }
                        buffer != null -> {
                            val outputEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            val outputBuffer =
                                if (outputEos && totalSize != null && totalSize < Constants.MINIMUM_SAMPLE_SIZE) {
                                    // TODO: Is this necessary?
                                    val elementsToAdd = min(
                                        Constants.MINIMUM_SAMPLE_SIZE - totalSize,
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
                            /**
                             * Dont forget to run codec.releaseOutputBuffer(index, false) when done with it!
                             */
                            return Triple(
                                if (outputEos) ProcessOutputResult.EOS else ProcessOutputResult.SUCCESS,
                                outputBuffer, index)
                        }
                        else -> return Triple(ProcessOutputResult.NO_BUFFER, null, null)
                    }
                }
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val (hasChanged, audioFormat) = getAudioFormat(codec.outputFormat, outputAudioFormat)
                    if (hasChanged) outputAudioFormat = audioFormat
                    return Triple(ProcessOutputResult.OUTPUT_FORMAT_CHANGED, null, null)
                }
                else -> return Triple(ProcessOutputResult.NO_BUFFER, null, null)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error in codec output, sound=$sound", e)
            return Triple(ProcessOutputResult.ERROR, null, null)
        }
    }

    private fun rebuildAudioTrack(): AudioTrack? {
        audioTrack?.release()
        // effect?.release()
        return try {
            buildAudioTrack()
        } catch (e: Exception) {
            onError("Error building audio track", e)
            null
        }
    }

    private fun writeAudioTrack(buffer: ByteBuffer): Int {
        /**
         * Will only write to AudioTrack if state is INIT_PLAY, PRIMING, or PLAYING
         * Returns number of bytes written
         */
        val sampleSize = buffer.remaining()

        audioTrack?.write(buffer, sampleSize, AudioTrack.WRITE_BLOCKING)?.also { result ->
            when (result) {
                AudioTrack.ERROR_BAD_VALUE -> onWarning("Audio output: bad value")
                AudioTrack.ERROR_DEAD_OBJECT -> onWarning("Audio output: dead object")
                AudioTrack.ERROR_INVALID_OPERATION -> onWarning("Audio output: not properly initialized")
                AudioTrack.ERROR -> onWarning("Error outputting audio")
                else -> {
                    val overshoot = sampleSize - result
                    if (overshoot > 0) {
                        if (BuildConfig.DEBUG)
                            Log.w(LOG_TAG,
                                "writeAudioTrack: wrote $result bytes, buffer=$buffer, overshoot=$overshoot, state=$state, sampleSize=$sampleSize, sound=$sound")
                        overflowBuffers.add(ByteBuffer.allocateDirect(overshoot).put(buffer).also { it.position(0) })
                    }
                    return result
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
     * PAUSED
     *    Means: Sound has been playing but has been paused by user input
     *    Begins: When user pressed pause
     *    Ends: On play(), prepare(), or release()
     *    May change from: PLAYING
     *    // TODO: FINISH THIS
     *    Changes to: PLAYING, ...
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
    enum class State { CREATED, INITIALIZING, READY, INIT_PLAY, PLAYING, PAUSED, STOPPED, ERROR, RELEASED }

    enum class ProcessInputResult { CONTINUE, END_NEXT, END }

    enum class ProcessOutputResult { SUCCESS, OUTPUT_FORMAT_CHANGED, CODEC_CONFIG, NO_BUFFER, EOS, ERROR, }

    companion object {
        const val LOG_TAG = "AudioFile"
    }
}