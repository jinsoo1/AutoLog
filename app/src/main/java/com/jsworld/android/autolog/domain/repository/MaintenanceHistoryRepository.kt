package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import kotlinx.coroutines.flow.Flow

interface MaintenanceHistoryRepository {
    /** 차량의 모든 정비 기록을 항목 이름과 함께 최신순으로 관찰한다(정비 탭 통합 타임라인). */
    fun observeCarRecords(carId: Long): Flow<List<CarMaintenanceRecord>>

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
