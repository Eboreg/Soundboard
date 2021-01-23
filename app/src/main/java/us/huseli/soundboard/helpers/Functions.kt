package us.huseli.soundboard.helpers

import kotlin.math.log2
import kotlin.math.pow

object Functions {
    fun seekbarValueToBufferSize(value: Int) = (11025 * 2.0.pow(value.toDouble())).toInt()

    fun bufferSizeToSeekbarValue(value: Int) = log2(value.toDouble() / 11025).toInt()
}