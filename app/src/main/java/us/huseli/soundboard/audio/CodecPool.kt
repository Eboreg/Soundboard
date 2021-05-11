package us.huseli.soundboard.audio

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import us.huseli.soundboard.BuildConfig
import java.nio.ByteBuffer

object CodecPool {
    const val LOG_TAG = "CodecPool"

    private val codecs = mutableListOf<Codec>()
    private val codecMutex = Mutex()

    suspend fun acquire(format: MediaFormat): Codec? = codecMutex.withLock {
        return (codecs.firstOrNull { it.matches(format) && !it.isBusy } ?: create(format))?.also { it.isBusy = true }
            .also {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "acquire: returning $it, codecs.size=${codecs.size}")
            }
    }

    suspend fun initialize(format: MediaFormat) {
        return codecMutex.withLock {
            if (codecs.filter { it.matches(format) }.size < 10) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                    "initialize: <10 instances for format=$format, creating another one")
                create(format)
            } else if (BuildConfig.DEBUG) Log.d(LOG_TAG,
                "initialize: >= 10 instances for format=$format, not creating more")
        }
    }

    private fun create(format: MediaFormat): Codec? {
        val codec = Codec.create(format)
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "create: creating, returning and adding $codec")
        if (codec != null) codecs.add(codec)
        return codec
    }

    class Codec(format: MediaFormat) {
        private val mediaCodec: MediaCodec? = try {
            val name = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format)
            MediaCodec.createByCodecName(name).also {
                it.configure(format, null, null, 0)
                it.start()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "init", e)
            null
        }
        private val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        private val mime = format.getString(MediaFormat.KEY_MIME)
        val outputFormat: MediaFormat? = mediaCodec?.outputFormat
        var isBusy = false
            set(value) {
                if (BuildConfig.DEBUG) Log.d(LOG_TAG, "isBusy: Setting to $value for $this")
                field = value
            }

        fun matches(format: MediaFormat) = format.getString(MediaFormat.KEY_MIME) == mime
                && format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == channelCount

        fun dequeueInputBuffer(timeoutUs: Long): Int? {
            return try {
                mediaCodec?.dequeueInputBuffer(timeoutUs)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "dequeueInputBuffer", e)
                null
            }
        }

        fun getInputBuffer(index: Int): ByteBuffer? {
            return try {
                mediaCodec?.getInputBuffer(index)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "getInputBuffer")
                null
            }
        }

        fun queueInputBuffer(index: Int, offset: Int, size: Int, presentationTimeUs: Long, flags: Int) {
            try {
                mediaCodec?.queueInputBuffer(index, offset, size, presentationTimeUs, flags)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "queueInputBuffer", e)
            }
        }

        fun dequeueOutputBuffer(info: MediaCodec.BufferInfo, timeoutUs: Long): Int? {
            return try {
                mediaCodec?.dequeueOutputBuffer(info, timeoutUs)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "dequeueOutputBuffer", e)
                null
            }
        }

        fun getOutputBuffer(index: Int): ByteBuffer? {
            return try {
                mediaCodec?.getOutputBuffer(index)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "getOutputBuffer", e)
                null
            }
        }

        fun releaseOutputBuffer(index: Int, render: Boolean) {
            try {
                mediaCodec?.releaseOutputBuffer(index, render)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "releaseOutoutBuffer", e)
            }
        }

        fun flush() {
            mediaCodec?.flush()
            isBusy = false
        }

        companion object {
            fun create(format: MediaFormat): Codec? {
                return try {
                    Codec(format)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}