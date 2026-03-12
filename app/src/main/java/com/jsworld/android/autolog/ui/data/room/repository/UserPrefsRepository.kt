package com.jsworld.android.autolog.ui.data.room.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UserPrefsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private fun autoMileageKey(carId: Long) =
        booleanPreferencesKey("auto_mileage_update_$carId")

    fun observeAutoMileageUpdate(carId: Long): Flow<Boolean> =
        dataStore.data.map { it[autoMileageKey(carId)] ?: false }

    suspend fun setAutoMileageUpdate(carId: Long, enabled: Boolean) {
        dataStore.edit { it[autoMileageKey(carId)] = enabled }
    }
}