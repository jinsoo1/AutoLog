package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.domain.model.CarMaintenanceDigest
import com.jsworld.android.autolog.domain.model.CarMaintenanceSetting
import com.jsworld.android.autolog.domain.model.MaintenanceSort
import com.jsworld.android.autolog.domain.model.MaintenanceTypePickUi
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.SettingOption
import com.jsworld.android.autolog.domain.model.SettingWithHistory
import kotlinx.coroutines.flow.Flow

interface CarMaintenanceRepository {
    fun getMaintenanceSettingsSorted(carId: Long, sort: MaintenanceSort): Flow<List<CarMaintenanceSetting>>
    fun observeSort(carId: Long): Flow<MaintenanceSort>
    fun observeMaintenanceStatusList(carId: Long): Flow<List<MaintenanceUiModel>>
    fun getSettingsWithHistory(carId: Long): Flow<List<SettingWithHistory>>
    fun observeSettingOptions(carId: Long): Flow<List<SettingOption>>
    suspend fun insertHistory(
        settingId: Long,
        serviceDate: String?,
        serviceMileage: Int?,
        place: String?,
        cost: Int?,
        memo: String?
    )
    fun observeSetting(settingId: Long): Flow<CarMaintenanceSetting?>
    suspend fun updateSettingIntervals(settingId: Long, km: Int?, months: Int?)
    fun observePickerItems(carId: Long): Flow<List<MaintenanceTypePickUi>>
    suspend fun setTypeEnabled(carId: Long, typeId: Long, enabled: Boolean)
    fun observeAllByCarId(carId: Long): Flow<List<CarMaintenanceSetting>>
    fun observeActiveByCarId(carId: Long): Flow<List<CarMaintenanceSetting>>
    suspend fun setActive(settingId: Long, active: Boolean)
    suspend fun getByCarIdAndTypeIdOnce(carId: Long, typeId: Long): CarMaintenanceSetting?
    suspend fun insertDefaultActive(carId: Long, typeId: Long): Long
    fun getHistoryById(id: Long): Flow<MaintenanceHistoryEntity?>
    fun getHistoriesForSetting(settingId: Long): Flow<List<MaintenanceHistoryEntity>>
    suspend fun updateHistory(entity: MaintenanceHistoryEntity)
    suspend fun deleteHistory(historyId: Long)
    suspend fun getCarIdBySettingId(settingId: Long): Long
    suspend fun updateCarMileage(carId: Long, mileage: Int)
    fun observeMaintenanceDigestForCarList(carId: Long): Flow<CarMaintenanceDigest>
    suspend fun enableTypeForCar(carId: Long, typeId: Long, intervalKm: Int?, intervalMonths: Int?)
    suspend fun addMaintenanceTypeAndEnableForCar(
        carId: Long,
        name: String,
        defaultKm: Int?,
        defaultMonths: Int?,
        carIntervalKm: Int?,
        carIntervalMonths: Int?
    ): Long
    suspend fun addMaintenanceTypeAndEnableForCarRejectDuplicate(
        carId: Long,
        name: String,
        defaultKm: Int?,
        defaultMonths: Int?,
        useCarOverride: Boolean,
        carIntervalKm: Int?,
        carIntervalMonths: Int?
    ): Long
}
