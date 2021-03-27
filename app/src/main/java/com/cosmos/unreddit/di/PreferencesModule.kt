package com.cosmos.unreddit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PreferencesModule {

    private const val PREFERENCES_NAME = "preferences"

    private val Context.preferencesDataStore by preferencesDataStore(name = PREFERENCES_NAME)

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.preferencesDataStore
    }
}
