package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.MaintenanceSort
import kotlinx.coroutines.flow.Flow

interface CarSortPreferenceRepository {
    fun observeSort(carId: Long): Flow<MaintenanceSort>
    suspend fun setSort(carId: Long, sort: MaintenanceSort)
}
