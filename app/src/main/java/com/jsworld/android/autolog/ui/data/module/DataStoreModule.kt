package com.jsworld.android.autolog.ui.data.module

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

val Context.prefsDataStore by preferencesDataStore(name = "autolog_prefs")


@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePrefsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.prefsDataStore

}