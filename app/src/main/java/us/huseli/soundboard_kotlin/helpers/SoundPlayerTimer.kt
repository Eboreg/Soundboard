package us.huseli.soundboard_kotlin.helpers

import android.os.CountDownTimer
import android.util.Log
import android.widget.ProgressBar
import kotlin.math.roundToInt

class SoundPlayerTimer(val duration: Int, private val progressBar: ProgressBar, private val originalProgress: Int) :
        CountDownTimer(duration.toLong(), 100) {
    //private val originalProgress = progressBar.progress

    private fun toPercentage(millisUntilFinished: Long) =
            (((duration - millisUntilFinished).toDouble() / duration) * 100).roundToInt()

    override fun onTick(millisUntilFinished: Long) {
        progressBar.progress = toPercentage(millisUntilFinished)
    }

    override fun onFinish() {
        if (duration < 0) Log.w(LOG_TAG, "Duration is negative; not initialized properly?")
        progressBar.progress = originalProgress
    }


    companion object {
        const val LOG_TAG = "SoundPlayerTimer"
    }
}