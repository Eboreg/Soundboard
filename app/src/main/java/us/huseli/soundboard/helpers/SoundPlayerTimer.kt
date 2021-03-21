package us.huseli.soundboard.helpers

import android.os.CountDownTimer
import android.util.Log
import android.widget.ProgressBar
import kotlin.math.roundToInt

class SoundPlayerTimer(private var duration: Long,
                       private val progressBar: ProgressBar,
                       private var originalProgress: Int) {
    constructor(duration: Int, progressBar: ProgressBar, originalProgress: Int) : this(duration.toLong(),
        progressBar,
        originalProgress)

    private var timer = CountDownTimerImpl(duration)
    private var millisLeft = duration
    private val percentage: Int
        get() = (((duration - millisLeft).toDouble() / duration) * 100).roundToInt()

    fun setDuration(value: Int) = setDuration(value.toLong())

    fun setOriginalProgress(value: Int) {
        originalProgress = value
    }

    fun start() {
        timer.start()
        Log.d(LOG_TAG, "start: millisLeft=$millisLeft, duration=$duration, timer=$timer")
    }

    fun stop() {
        timer.cancel()
        onFinish()
        Log.d(LOG_TAG, "stop: millisLeft=$millisLeft, duration=$duration, timer=$timer")
    }

    fun pause() = timer.cancel()

    fun resume() {
        if (millisLeft != duration) timer = CountDownTimerImpl(millisLeft)
        timer.start()
    }

    private fun setDuration(value: Long) {
        if (value != duration) {
            duration = value
            timer = CountDownTimerImpl(duration)
        }
    }

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