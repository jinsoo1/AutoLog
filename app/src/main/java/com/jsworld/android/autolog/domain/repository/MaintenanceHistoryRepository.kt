package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import kotlinx.coroutines.flow.Flow

interface MaintenanceHistoryRepository {
    suspend fun getMaxMileageForCar(carId: Long): Int?
    fun observeLastHistory(settingId: Long): Flow<MaintenanceHistory?>
    suspend fun insert(
        settingId: Long,
        serviceDate: String,
        serviceMileage: Int,
        place: String?,
        cost: Int?,
        memo: String?
    )
    fun observeHistories(settingId: Long): Flow<List<MaintenanceHistory>>
    fun observeMaxServiceMileageForCar(carId: Long): Flow<Int?>
}
