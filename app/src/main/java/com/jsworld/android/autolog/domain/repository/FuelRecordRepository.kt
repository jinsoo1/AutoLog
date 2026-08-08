package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyFuelCost
import kotlinx.coroutines.flow.Flow

interface FuelRecordRepository {

    fun observeByCar(carId: Long): Flow<List<FuelRecord>>

    fun observeById(recordId: Long): Flow<FuelRecord?>

    fun observeMonthlyCost(carId: Long): Flow<List<MonthlyFuelCost>>

    /** 입력 화면의 최근 주유소/충전소 제안. 종류가 다르면 장소도 다르므로 unit 별로 낸다. */
    fun observeRecentStations(carId: Long, unit: FuelUnit): Flow<List<String>>

    /** 직전 주유 기록의 주행거리 */
    suspend fun getLatestMileage(carId: Long): Int?

    suspend fun insert(
        carId: Long,
        filledAt: String,
        mileage: Int?,
        amount: Int?,
        quantity: Double?,
        unitPrice: Int?,
        unit: FuelUnit,
        station: String?,
        memo: String?,
        photoPath: String? = null
    )

    suspend fun update(record: FuelRecord)

    suspend fun delete(recordId: Long)
}
