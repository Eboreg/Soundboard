package us.huseli.soundboard.helpers

import android.media.AudioFormat
import android.media.MediaFormat
import android.os.Build
import android.os.Looper
import android.util.Log
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

    fun audioFormatsEqual(format1: AudioFormat, format2: AudioFormat) =
        format1.channelMask == format2.channelMask && format1.encoding == format2.encoding

    fun warnIfOnMainThread(caller: String) {
        if (Looper.getMainLooper().thread == Thread.currentThread())
            Log.e("warnIfOnMainThread", "$caller was called from main thread, but it shouldn't be!")
    }

    private fun umlautify(char: Char): Char {
        val newChar = when(char.lowercaseChar()) {
            'a' -> 'ä'
            'e' -> 'ë'
            'i' -> 'ï'
            'o' -> 'ö'
            'u' -> 'ü'
            'y' -> 'ÿ'
            else -> char
        }
        return if (char.isUpperCase()) newChar.uppercaseChar() else newChar
    }

    /**
     * Randomly changes vowels into their cool ümläüt counterparts.
     * `probability` (0.0 - 1.0) sets the probability that this will happen to
     * any individual vowel. However, at least one vowel will always be
     * changed.
     */
    fun umlautify(string: CharSequence, probability: Double = 0.3): CharSequence {
        val vowels = string
            .mapIndexedNotNull { idx, char ->
                if ("aeiouy".contains(char, true)) Pair(idx, char) else null
            }
            .shuffled()
        val correctedProbability = ((probability * vowels.size) - 1) / (vowels.size - 1)
        var newString = string
        var first = true

        vowels.forEach { pair ->
            if (first || Math.random() < correctedProbability) {
                newString =
                    newString.substring(0, pair.first) +
                    umlautify(pair.second) +
                    newString.substring(pair.first + 1)
                first = false
            }
        }

        return newString
    }
}