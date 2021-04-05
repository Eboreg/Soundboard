package us.huseli.soundboard.audio

import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import us.huseli.soundboard.BuildConfig
import us.huseli.soundboard.data.Sound
import us.huseli.soundboard.helpers.Functions
import java.nio.ByteBuffer

/**
 * Design decision: `state` is only set in the public methods, including callbacks defined by them.
 * Exception: onError() sets state = State.ERROR.
 *
 * Valid state should also only be checked by the public methods.
 * Exceptions:
 *  - extractEncoded() and extractRaw() check State.INIT_PLAY, to be able to time on-playing-callback as accurately
 *    as possible
 */

class AudioFile(private val sound: Sound, volume: Int, baseBufferSize: Int, listener: Listener? = null) {
    constructor(sound: Sound, baseBufferSize: Int, listener: Listener?) :
            this(sound, sound.volume, baseBufferSize, listener)

    // Public val's & var's
    val duration: Long
    val isPlaying: Boolean
        get() = state == State.PLAYING

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
    // private var audioTrack: AudioTrack? = null
    private var codec: MediaCodec? = null
    private var extractJob: Job? = null
    private var extractorDone = false
    private var playAction: PlayAction? = null
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

    suspend fun pause(): AudioFile {
        playAction?.also { it.pause() } ?: onWarning("Pause: No active sound")
        playAction = null
        return this
    }

    suspend fun play(timeoutUs: Long? = null): AudioFile {
        playAction = Play().start(timeoutUs)
        return this
    }

    suspend fun playAndPrepare(): AudioFile {
        playAction = PlayAndPrepare().start()
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

    suspend fun prepareAndPrime(): AudioFile {
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
            playAction?.release()
            playAction = null
            codec?.release()
            stateListener = null
            // audioTrack = null
            codec = null
            primedData = null
            scope.cancel()
        }
        return this
    }

    suspend fun restartAndPrepare(timeoutUs: Long): AudioFile {
        playAction = if (state == State.PLAYING || state == State.INIT_PLAY)
            RestartAndPrepare().start()
        else PlayAndPrepare().start(timeoutUs)
        return this
    }

    suspend fun resumeAndPrepare(): AudioFile {
        /** When sound is paused and should start playing again */
        playAction = ResumeAndPrepare().start()
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
                Log.e(LOG_TAG, "flush codec error, sound=$sound", e)
            }
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


    abstract inner class PlayAction {
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
        suspend fun pause(): PlayAction {
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

        suspend fun start(timeoutUs: Long? = null): PlayAction {
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
                    log("start: buffer=$buffer")
                    val writeResult = audioTrack.write(buffer)
                    log("writeResult=$writeResult")

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

        suspend fun stop(): PlayAction {
            /** Used for user-initiated hard stop, cancels any queued future stop job */
            log("stop called")
            if (state == State.PLAYING || state == State.INIT_PLAY) {
                queuedStopJob?.cancel()
                onStopped()
            } else onWarning("Stop: Illegal state", "stop: illegal state $state, should be PLAYING or INIT_PLAY")
            return this
        }
    }

    inner class Play : PlayAction() {
        override val validStartStates = listOf(State.READY)

        override suspend fun onStopped() {
            log("onStopped called")
            extractJob?.cancelAndJoin()
            audioTrack.release()
            state = State.STOPPED
        }
    }

    inner class PlayAndPrepare : PlayAction() {
        override val validStartStates = listOf(State.READY)
    }

    inner class RestartAndPrepare : PlayAction() {
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

    inner class ResumeAndPrepare : PlayAction() {
        /** When sound is paused and should start playing again */
        override val stopAfterDelay = duration - framesToMilliseconds(audioTrack.playbackHeadPosition)
        override val validStartStates = listOf(State.PAUSED)

        override suspend fun preparePlay() {
            audioTrack.resume()
        }
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

    companion object {
        const val LOG_TAG = "AudioFile"
    }
}