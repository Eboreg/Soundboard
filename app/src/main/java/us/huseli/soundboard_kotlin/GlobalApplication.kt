package us.huseli.soundboard_kotlin

import android.app.Application
import android.content.Context

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        application = this
        context = applicationContext
    }

    companion object {
        lateinit var context: Context
        lateinit var application: GlobalApplication
    }
}