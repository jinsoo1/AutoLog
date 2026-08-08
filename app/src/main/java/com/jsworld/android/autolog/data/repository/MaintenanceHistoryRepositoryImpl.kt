package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.domain.repository.MaintenanceHistoryRepository

import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.MaintenanceHistory
import com.jsworld.android.autolog.data.local.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.mapper.toDomain
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaintenanceHistoryRepositoryImpl @Inject constructor(
    private val historyDao: MaintenanceHistoryDao
) : MaintenanceHistoryRepository {
    override suspend fun getMaxMileageForCar(carId: Long): Int? =
        historyDao.getMaxServiceMileageForCar(carId)

    override fun observeLastHistory(settingId: Long): Flow<MaintenanceHistory?> {
        return historyDao.observeLastHistory(settingId)
            .map { it?.toDomain() }
    }

    override suspend fun insert(
        settingId: Long,
        serviceDate: String,
        serviceMileage: Int,
        place: String?,
        cost: Int?,
        memo: String?
    ) {
        // 최소 방어 로직 (화면에서 이미 검증하지만, repository에서도 한번 더)
        require(settingId > 0) { "settingId must be > 0" }
        require(serviceDate.isNotBlank()) { "serviceDate is required" }
        require(serviceMileage > 0) { "serviceMileage must be > 0" }

        val history = MaintenanceHistoryEntity(
            settingId = settingId,
            serviceDate = serviceDate.trim(),
            serviceMileage = serviceMileage,
            place = place?.trim()?.takeIf { it.isNotBlank() },
            cost = cost,
            memo = memo?.trim()?.takeIf { it.isNotBlank() }
        )

        historyDao.insertHistory(history)

    }

    override fun observeHistories(settingId: Long): Flow<List<MaintenanceHistory>> {
        return historyDao.observeHistories(settingId)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeMaxServiceMileageForCar(carId: Long): Flow<Int?> {
        return historyDao.observeMaxServiceMileageForCar(carId)
    }

    override fun observeCarRecords(carId: Long): Flow<List<CarMaintenanceRecord>> {
        return historyDao.observeCarRecords(carId)
            .map { list -> list.map { it.toDomain() } }
    }


}