package us.huseli.soundboard.helpers

import android.util.Log
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** Heavily "inspired" by https://stackoverflow.com/a/14922433/14311882 */
object MD5 {
    const val LOG_TAG = "MD5"

    fun calculate(inputStream: InputStream): String? {
        val buf = ByteArray(8192)
        var len: Int

        try {
            val digest = MessageDigest.getInstance("MD5")
            while (inputStream.read(buf).also { len = it } > 0) {
                digest.update(buf, 0, len)
            }
            val bigInt = BigInteger(1, digest.digest())
            return String.format("%32s", bigInt.toString(16)).replace(" ", "0")
        } catch (e: NoSuchAlgorithmException) {
            Log.e(LOG_TAG, "MessageDigest: No such algorithm ($e)")
            return null
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Unable to process file for MD5: $e")
        }
        return null
    }

    fun calculate(file: File): String? {
        val inputStream: InputStream
        try {
            inputStream = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            Log.e(LOG_TAG, "File not found: $file")
            return null
        }
        return calculate(inputStream).also { inputStream.close() }
    }

    /** Maybe I'll not use this, but it's good to have anyway */
    @Suppress("unused")
    fun check(md5: String, file: File) = calculate(file)?.equals(md5, ignoreCase = true) ?: false
}