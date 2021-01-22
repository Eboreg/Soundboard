package us.huseli.soundboard.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.soundboard.helpers.ColorHelper
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ColorHelperModule {
    @Provides
    @Singleton
    fun provideColorHelper(@ApplicationContext appContext: Context) = ColorHelper(appContext.resources)
}