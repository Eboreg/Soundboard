package us.huseli.soundboard.di

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AudioFileThreadModule {
    @Provides
    @Singleton
    fun provideAudioFileThread() = HandlerThread("audioFile", Process.THREAD_PRIORITY_URGENT_AUDIO).also {
        it.start()
    }

    @Provides
    @Singleton
    fun provideHandler(thread: HandlerThread) = Handler(thread.looper)
}