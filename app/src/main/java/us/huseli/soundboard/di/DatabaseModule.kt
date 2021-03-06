package us.huseli.soundboard.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.huseli.soundboard.data.SoundboardDatabase
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context) = SoundboardDatabase.buildDatabase(appContext)

    @Provides
    @Singleton
    fun provideSoundDao(database: SoundboardDatabase) = database.soundDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: SoundboardDatabase) = database.categoryDao()
}