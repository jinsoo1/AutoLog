package com.jsworld.android.autolog.data.repository

import com.jsworld.android.autolog.data.local.dao.FuelRecordDao
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.mapper.toDomain
import com.jsworld.android.autolog.data.mapper.toEntity
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyFuelCost
import com.jsworld.android.autolog.domain.repository.FuelRecordRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FuelRecordRepositoryImpl @Inject constructor(
    private val fuelRecordDao: FuelRecordDao
) : FuelRecordRepository {

    override fun observeByCar(carId: Long): Flow<List<FuelRecord>> =
        fuelRecordDao.observeByCar(carId).map { list -> list.map { it.toDomain() } }

    override fun observeById(recordId: Long): Flow<FuelRecord?> =
        fuelRecordDao.observeById(recordId).map { it?.toDomain() }

    override fun observeMonthlyCost(carId: Long): Flow<List<MonthlyFuelCost>> =
        fuelRecordDao.observeMonthlyCost(carId).map { list -> list.map { it.toDomain() } }

    override fun observeRecentStations(carId: Long, unit: FuelUnit): Flow<List<String>> =
        fuelRecordDao.observeRecentStations(carId, unit.symbol)

    override suspend fun getLatestMileage(carId: Long): Int? =
        fuelRecordDao.getLatestMileage(carId)

    override suspend fun getMileageAround(carId: Long, date: String): Pair<Int?, Int?> =
        fuelRecordDao.getMileageOnOrBefore(carId, date) to
            fuelRecordDao.getMileageAfter(carId, date)

    override suspend fun insert(
        carId: Long,
        filledAt: String,
        mileage: Int?,
        amount: Int?,
        quantity: Double?,
        unitPrice: Int?,
        unit: FuelUnit,
        station: String?,
        memo: String?,
        photoPath: String?
    ) {
        require(carId > 0) { "carId must be > 0" }
        require(filledAt.isNotBlank()) { "filledAt is required" }

        fuelRecordDao.insert(
            FuelRecordEntity(
                carId = carId,
                filledAt = filledAt.trim(),
                mileage = mileage,
                amount = amount,
                quantity = quantity,
                unitPrice = unitPrice,
                unit = unit.symbol,
                station = station?.trim()?.takeIf { it.isNotBlank() },
                memo = memo?.trim()?.takeIf { it.isNotBlank() },
                photoPath = photoPath
            )
        )
    }

    override suspend fun update(record: FuelRecord) {
        fuelRecordDao.update(record.toEntity())
    }

    override suspend fun delete(recordId: Long) {
        fuelRecordDao.deleteById(recordId)
    }
}
