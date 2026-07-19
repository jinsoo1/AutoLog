package com.jsworld.android.autolog.data.datastore

import com.jsworld.android.autolog.domain.repository.CarSortPreferenceRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jsworld.android.autolog.domain.model.MaintenanceSort
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CarSortPreferenceRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : CarSortPreferenceRepository {
    private fun sortKey(carId: Long) =
        stringPreferencesKey("car_sort_$carId")

    override fun observeSort(carId: Long): Flow<MaintenanceSort> =
        dataStore.data.map { prefs ->
            val raw = prefs[sortKey(carId)]
            runCatching { MaintenanceSort.valueOf(raw ?: "") }.getOrDefault(MaintenanceSort.DEFAULT)
        }

    override suspend fun setSort(carId: Long, sort: MaintenanceSort) {
        dataStore.edit { it[sortKey(carId)] = sort.name }
    }
}