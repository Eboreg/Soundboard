package us.huseli.soundboard.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.huseli.soundboard.audio.AudioTrackProvider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AudioTrackProviderModule {
    @Provides
    @Singleton
    fun audioTrackProvider() = AudioTrackProvider()
}