package us.huseli.soundboard.audio

import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.helpers.Functions
import java.nio.ByteBuffer

class AudioFile(private val path: String, volume: Int, baseBufferSize: Int, listener: Listener? = null) {

    // Public val's & var's
    val duration: Long
    val isPaused
        get() = state == State.PAUSED
    val isPlaying
        get() = state == State.PLAYING
    val playbackPositionMs
        get() = framesToMilliseconds(audioTrack.playbackHeadPosition)

    // Private val's to be initialized in init
    private val audioTrack: AudioTrackContainer
    private val inputMediaFormat: MediaFormat
    private val mime: String

    // Private val's initialized here
    private val extractor = MediaExtractor()
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private var stateListener = listener

    // Private var's to be initialized later on
    private var bufferSize: Int
    private var channelCount: Int
    private var outputAudioFormat: AudioFormat

    // Private var's initialized here
    private var codec: MediaCodec? = null
    private var extractJob: Job? = null
    private var extractorDone = false
    private var playAction: PlayActionAbstract? = null
    private var primedData: ByteBuffer? = null
    private var queuedStopJob: Job? = null

    private var state = State.CREATED
        set(value) {
            // We will not change from RELEASED to anything else, because RELEASED is final
            if (field != value && field != State.RELEASED) {
                // Except for on RELEASED, we will not change from ERROR to anything else
                if (field != State.ERROR) {
                    if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                        "state changed from $field to $value, this=$this, path=$path")
                    field = value
                    stateListener?.onAudioFileStateChange(value)
                }
            }
        }

    init {
        inputMediaFormat = initExtractor() ?: run {
            onError("Could not get media type")
            throw Exception("Could not get media type")
        }

        // InputFormat duration is in MICROseconds!
        duration = (inputMediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000)
        mime = inputMediaFormat.getString(MediaFormat.KEY_MIME) ?: run {
            onError("Could not get MIME type")
            throw Exception("Could not get MIME type")
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        channelCount = inputMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        bufferSize = baseBufferSize * channelCount
        outputAudioFormat = Functions.mediaFormatToAudioFormat(inputMediaFormat)
        audioTrack = AudioTrackContainer(audioAttributes, outputAudioFormat, bufferSize, volume)
    }

    /********** PUBLIC METHODS **********/

    fun changeBufferSize(baseBufferSize: Int): AudioFile {
        if (baseBufferSize * channelCount != bufferSize) {
            bufferSize = baseBufferSize * channelCount
            audioTrack.setBufferSize(bufferSize)
            if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "changeBufferSize: baseBufferSize=$baseBufferSize, bufferSize=$bufferSize")
        }
        return this
    }

    fun changeVolume(value: Int) = audioTrack.setVolume(value)

    fun onStateChanged(callback: (state: State) -> Unit): AudioFile {
        stateListener = SimpleStateListener(callback)
        return this
    }

    suspend fun pause(): AudioFile {
        playAction?.also { it.pause() } ?: onWarning("Pause: No active sound")
        playAction = null
        return this
    }

    suspend fun play(timeoutUs: Long? = null): AudioFile {
        playAction = PlayAction().start(timeoutUs)
        return this
    }

    fun prepare(): AudioFile {
        when (state) {
            State.CREATED, State.STOPPED, State.RELEASED, State.PAUSED -> {
                state = State.INITIALIZING
                doPrepare()
                state = State.READY
            }
            else -> onWarning(
                "Prepare: Illegal state",
                "prepare: illegal state $state, should be CREATED, STOPPED, RELEASED, PAUSED")
        }
        return this
    }

    suspend fun prepareAndPrime(): AudioFile {
        when (state) {
            State.CREATED, State.STOPPED, State.RELEASED, State.PAUSED -> {
                state = State.INITIALIZING
                doPrepare()
                doPrime()
                state = State.READY
            }
            else -> onWarning(
                "Prepare: Illegal state",
                "prepareAndPrime: illegal state $state, should be CREATED, STOPPED, RELEASED, PAUSED")
        }
        return this
    }

    fun release(): AudioFile {
        if (state != State.RELEASED) {
            state = State.RELEASED
            playAction?.release()
            playAction = null
            codec?.release()
            stateListener = null
            codec = null
            primedData = null
            scope.cancel()
        }
        return this
    }

    suspend fun restart(timeoutUs: Long): AudioFile {
        playAction = when (state) {
            State.PLAYING, State.INIT_PLAY -> RestartAction().start()
            else -> PlayAction().start(timeoutUs)
        }
        return this
    }

    suspend fun resume(): AudioFile {
        /** When sound is paused and should start playing again */
        playAction = ResumeAction().start()
        return this
    }

    suspend fun stop(): AudioFile {
        /**
         * Used for user-initiated hard stop, cancels any queued future stop job
         */
        playAction?.also { it.stop() } ?: onWarning("Stop: No active sound")
        playAction = null
        return this
    }


    /********** PRIVATE METHODS **********/

    private fun doPrepare() {
        if (BuildConfig.DEBUG) Functions.warnIfOnMainThread("doPrepare")

        extractorDone = false
        extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        if (mime != MediaFormat.MIMETYPE_AUDIO_RAW) initCodec()
    }

    private suspend fun doPrime() {
        if (BuildConfig.DEBUG) Functions.warnIfOnMainThread("doPrime")
        extractJob?.cancelAndJoin()
        primedData = AudioExtractor(audioTrack, extractor, mime, bufferSize, codec).prime()
    }

    private fun framesToMilliseconds(frames: Int): Long {
        // milliseconds = 1000 * frames / hz
        // 1 frame in 16 bit mono = 2 bytes, stereo = 4 bytes
        // Let's assume we always output 16 bit (== 2 bytes per sample) PCM for simplicity
        // TODO: Do we need to factor in stereo here or is that already accounted for?
        return ((1000 * frames) / outputAudioFormat.sampleRate).toLong()
    }

    private fun initCodec() {
        if (codec == null) {
            val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(inputMediaFormat)
            if (codecName != null) {
                try {
                    codec = MediaCodec.createByCodecName(codecName).also {
                        it.configure(inputMediaFormat, null, null, 0)
                        it.start()
                    }
                } catch (e: Exception) {
                    onError("Codec error")
                }
            } else onError("Could not find a suitable codec")
        } else {
            try {
                codec?.flush()
            } catch (e: IllegalStateException) {
                Log.e(LOG_TAG, "flush codec error, path=$path", e)
            }
        }
    }

    private fun initExtractor(): MediaFormat? {
        try {
            extractorDone = false
            extractor.setDataSource(path)
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
        Log.e(LOG_TAG, "message=$message, path=$path", exception)
        stateListener?.onAudioFileError(message)
    }

    private fun onWarning(message: String, verboseMessage: String? = null, exception: Exception? = null) {
        Log.w(LOG_TAG,
            "message=${verboseMessage ?: message}, exception=$exception, path=$path", exception)
        stateListener?.onAudioFileWarning(message)
    }


    /********** INNER CLASSES/INTERFACES/ENUMS **********/

    interface Listener {
        fun onAudioFileStateChange(state: State)
        fun onAudioFileError(message: String)
        fun onAudioFileWarning(message: String)
    }


    class SimpleStateListener(private val onStateChange: (state: State) -> Unit) : Listener {
        override fun onAudioFileStateChange(state: State) = onStateChange(state)
        override fun onAudioFileError(message: String) = Unit
        override fun onAudioFileWarning(message: String) = Unit
    }


    abstract inner class PlayActionAbstract {
        private var onPlayStartCalled = false
        abstract val validStartStates: List<State>
        open val stopAfterDelay: Long = duration

        /********** PRIVATE METHODS **********/
        private fun enqueueStop() {
            log("enqueueStop called, stopAfterDelay=$stopAfterDelay")
            queuedStopJob?.cancel()
            queuedStopJob = scope.launch {
                // Await end of stream, then stop
                delay(stopAfterDelay)
                // If playback head position is still 0, just give up
                if (audioTrack.playbackHeadPosition > 0) {
                    framesToMilliseconds(audioTrack.playbackHeadPosition).also { ms ->
                        if (ms < stopAfterDelay) delay(stopAfterDelay - ms)
                    }
                }
                state = State.STOPPED
                onStopped()
            }
        }

        private fun getBuffer(audioExtractor: AudioExtractor): ByteBuffer? {
            return primedData?.also { primedData = null } ?: audioExtractor.extractBuffer()
        }

        private fun isTimeoutReached(timeoutUs: Long?) = timeoutUs?.let { System.nanoTime() > it } == true

        private fun doOnPlayStart() {
            if (!onPlayStartCalled) {
                onPlayStartCalled = true
                onPlayStart()
                enqueueStop()
            }
        }

        /********** PROTECTED METHODS **********/
        protected fun log(string: String) {
            if (BuildConfig.DEBUG) Log.d(this.javaClass.simpleName, string)
        }

        protected open suspend fun onPlayFail() {
            log("onPlayFail called")
            extractJob?.cancelAndJoin()
            queuedStopJob?.cancel()
            doPrepare()
            start()
        }

        protected open fun onPlayStart() {
            log("onPlayStart called")
            state = State.PLAYING
        }

        protected open suspend fun onStopped() {
            log("onStopped called")
            state = State.INITIALIZING
            extractJob?.cancelAndJoin()
            audioTrack.release()
            doPrepare()
            doPrime()
            state = State.READY
        }

        protected open suspend fun onTimeout() {
            log("onTimeout called")
            queuedStopJob?.cancel()
            onWarning("Timeout")
            onStopped()
        }

        protected open suspend fun preparePlay() {
            state = State.INIT_PLAY
            audioTrack.build()
            audioTrack.play()
        }

        /********** PUBLIC METHODS **********/
        suspend fun pause(): PlayActionAbstract {
            log("pause called")
            if (state != State.PLAYING)
                onWarning("Pause: Illegal state", "pause: illegal state $state, should be PLAYING")
            else {
                extractJob?.cancelAndJoin()
                audioTrack.pause()
                queuedStopJob?.cancel()
                state = State.PAUSED
            }
            return this
        }

        fun release() {
            /** Used by AudioFile.release() */
            log("release called")
            extractJob?.cancel()
            queuedStopJob?.cancel()
            audioTrack.release()
        }

        suspend fun start(timeoutUs: Long? = null): PlayActionAbstract {
            if (!validStartStates.contains(state)) {
                onWarning(
                    "Play: Illegal state",
                    "${this.javaClass.simpleName}: illegal state $state, should be one of: ${validStartStates.joinToString()}")
                return this
            }
            val audioExtractor = AudioExtractor(audioTrack, extractor, mime, bufferSize, codec)
            preparePlay()
            if (isTimeoutReached(timeoutUs)) {
                onTimeout()
                return this
            }
            extractJob = scope.launch {
                val job = coroutineContext[Job]
                while (job?.isActive == true && !audioExtractor.isEosReached()) {
                    val buffer = getBuffer(audioExtractor)
                    // log("start: buffer=$buffer")
                    val writeResult = audioTrack.write(buffer)
                    // log("writeResult=$writeResult")

                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (writeResult.status) {
                        AudioTrackContainer.WriteStatus.FAIL -> onPlayFail()
                        AudioTrackContainer.WriteStatus.OK -> doOnPlayStart()
                        AudioTrackContainer.WriteStatus.ERROR -> onError(writeResult.message)
                    }
                }
            }
            return this
        }

        suspend fun stop(): PlayActionAbstract {
            /** Used for user-initiated hard stop, cancels any queued future stop job */
            log("stop called")
            if (state == State.PLAYING || state == State.INIT_PLAY) {
                queuedStopJob?.cancel()
                state = State.STOPPED
                onStopped()
            } else onWarning("Stop: Illegal state", "stop: illegal state $state, should be PLAYING or INIT_PLAY")
            return this
        }
    }

    inner class PlayAction : PlayActionAbstract() {
        override val validStartStates = listOf(State.READY)
    }

    inner class RestartAction : PlayActionAbstract() {
        override val validStartStates = listOf(State.PLAYING, State.INIT_PLAY)

        override suspend fun preparePlay() {
            state = State.INIT_PLAY
            extractJob?.cancelAndJoin()
            queuedStopJob?.cancel()
            audioTrack.flush()
            doPrepare()
            super.preparePlay()
        }
    }

    inner class ResumeAction : PlayActionAbstract() {
        /** When sound is paused and should start playing again */
        override val stopAfterDelay = duration - framesToMilliseconds(audioTrack.playbackHeadPosition)
        override val validStartStates = listOf(State.PAUSED)

        override suspend fun preparePlay() {
            audioTrack.resume()
        }
    }


    /**
     * All states may at any time change to RELEASED.
     * All states, except RELEASED, may at any time change to ERROR.
     *
     * CREATED
     *    Means: Is newly created, prepare() has not been run but needs to
     *    Begins: On creation
     *    Ends: When prepare() runs > INITIALIZING
     * INITIALIZING
     *    Means: Sound is initializing
     *    Begins: On prepare()
     *    Ends: When all is initialized > READY
     * READY
     *    Means: Everything is ready for playback
     *    Begins: When prepare() has finished
     *    Ends: When playback init starts -> INIT_PLAY
     * INIT_PLAY
     *    Means: User has pressed 'play', playback is initializing
     *    Begins: Directly on playback initialization
     *    Ends: When sound output has begun -> PLAYING
     * PLAYING
     *    Means: Sound is currently playing
     *    Begins: On first sound output
     *    Ends:
     *      On stop -> STOPPED
     *      On pause -> PAUSED
     * PAUSED
     *    Means: Sound has been playing but has been paused by user input
     *    Begins: When user pressed pause
     *    Ends:
     *      On play -> INIT_PLAY
     *      On prepare -> INITIALIZING
     * STOPPED
     *    Means: Playback has stopped and is not (yet) in initializing/ready state
     *    Begins: When user pressed stop or sound has played to the end
     *    Ends: On prepare -> INITIALIZING
     * RELEASED
     *    Means: Everything has been (or is being) released
     *    Begins: As soon as release() is run
     *    Ends: Never -- this is final
     * ERROR
     *    Means: Any non-recoverable error has occurred
     *    Begins: When onError() is run
     *    Ends: Only when release() is run
     */
    enum class State { CREATED, INITIALIZING, READY, INIT_PLAY, PLAYING, PAUSED, STOPPED, ERROR, RELEASED }

    companion object {
        const val LOG_TAG = "AudioFile"
    }
}