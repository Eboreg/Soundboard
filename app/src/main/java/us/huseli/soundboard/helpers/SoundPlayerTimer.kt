package us.huseli.soundboard.helpers

import android.os.CountDownTimer
import android.widget.ProgressBar
import kotlin.math.roundToInt

class SoundPlayerTimer(private var duration: Long,
                       private val progressBar: ProgressBar,
                       private var originalProgress: Int) {
    private var timer = CountDownTimerImpl(duration)
    private var millisLeft = duration
    private val percentage: Int
        get() = (((duration - millisLeft).toDouble() / duration) * 100).roundToInt()

    fun setDuration(value: Long) {
        if (value != duration) {
            duration = value
            timer = CountDownTimerImpl(duration)
        }
    }

    fun setOriginalProgress(value: Int) {
        originalProgress = value
    }

    fun start(millisDone: Long = 0) {
        if (millisDone > 0) {
            millisLeft = duration - millisDone
            timer = CountDownTimerImpl(millisLeft)
        }
        timer.start()
    }

    fun stop() {
        timer.cancel()
        onFinish()
    }

    fun pause(millisDone: Long = 0) {
        timer.cancel()
        if (millisDone > 0) {
            millisLeft = duration - millisDone
            timer = CountDownTimerImpl(millisLeft)
        }
        progressBar.progress = percentage
    }

/*
    fun resume() {
        if (millisLeft != duration) timer = CountDownTimerImpl(millisLeft)
        timer.start()
    }
*/

    private fun onFinish() {
        progressBar.progress = originalProgress
        millisLeft = duration
        if (timer.millisInFuture != duration) timer = CountDownTimerImpl(duration)
    }

    private fun onTick(millisUntilFinished: Long) {
        millisLeft = millisUntilFinished
        progressBar.progress = percentage
    }

    inner class CountDownTimerImpl(val millisInFuture: Long) : CountDownTimer(millisInFuture, 50) {
        override fun onTick(millisUntilFinished: Long) = this@SoundPlayerTimer.onTick(millisUntilFinished)

        override fun onFinish() = this@SoundPlayerTimer.onFinish()
    }

    companion object {
        const val LOG_TAG = "SoundPlayerTimer"
    }
}