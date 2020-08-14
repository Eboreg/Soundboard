package us.huseli.soundboard_kotlin

import android.app.Application

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {
        const val LOG_TAG = "sgrumf"

        lateinit var application: GlobalApplication
    }
}