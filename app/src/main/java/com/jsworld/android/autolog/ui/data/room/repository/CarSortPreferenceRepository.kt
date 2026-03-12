package com.jsworld.android.autolog.ui.data.room.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jsworld.android.autolog.ui.data.item.MaintenanceSort
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CarSortPreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private fun sortKey(carId: Long) =
        stringPreferencesKey("car_sort_$carId")

    fun observeSort(carId: Long): Flow<MaintenanceSort> =
        dataStore.data.map { prefs ->
            val raw = prefs[sortKey(carId)]
            runCatching { MaintenanceSort.valueOf(raw ?: "") }.getOrDefault(MaintenanceSort.DEFAULT)
        }

    suspend fun setSort(carId: Long, sort: MaintenanceSort) {
        dataStore.edit { it[sortKey(carId)] = sort.name }
    }
}