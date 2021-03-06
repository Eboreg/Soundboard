package us.huseli.soundboard.helpers

import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Build
import kotlin.math.log2
import kotlin.math.pow

object Functions {
    fun seekbarValueToBufferSize(value: Int) = (11025 * 2.0.pow(value.toDouble())).toInt()

    fun bufferSizeToSeekbarValue(value: Int) = log2(value.toDouble() / 11025).toInt()

    fun mediaFormatToAudioFormat(mediaFormat: MediaFormat): AudioFormat {
        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val inputChannelMask = if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_MASK))
            mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_MASK) else 0
        val inputChannelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val channelMask: Int? = when {
            inputChannelMask > 0 -> inputChannelMask
            inputChannelCount == 1 -> AudioFormat.CHANNEL_OUT_MONO
            inputChannelCount == 2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> null
        }
        val encoding: Int? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
                mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) else null

        val audioFormatBuilder = AudioFormat.Builder().setSampleRate(sampleRate)
        if (channelMask != null && channelMask > 0) audioFormatBuilder.setChannelMask(channelMask)
        if (encoding != null) audioFormatBuilder.setEncoding(encoding)
        return audioFormatBuilder.build()
    }
}