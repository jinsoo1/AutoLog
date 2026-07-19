package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity

@Dao
interface BackupDao {

    @Query("SELECT * FROM cars ORDER BY id ASC")
    suspend fun getAllCars(): List<CarEntity>

    @Query("SELECT * FROM maintenance_types ORDER BY id ASC")
    suspend fun getAllMaintenanceTypes(): List<MaintenanceTypeEntity>

    @Query("SELECT * FROM car_maintenance_settings ORDER BY id ASC")
    suspend fun getAllMaintenanceSettings(): List<CarMaintenanceSettingEntity>

    @Query("SELECT * FROM maintenance_history ORDER BY id ASC")
    suspend fun getAllMaintenanceHistories(): List<MaintenanceHistoryEntity>

    @Query("SELECT * FROM mileage_history ORDER BY id ASC")
    suspend fun getAllMileageHistories(): List<MileageHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCars(cars: List<CarEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMaintenanceTypes(
        types: List<MaintenanceTypeEntity>
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMaintenanceSettings(
        settings: List<CarMaintenanceSettingEntity>
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMaintenanceHistories(
        histories: List<MaintenanceHistoryEntity>
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMileageHistories(
        histories: List<MileageHistoryEntity>
    )

    @Query("DELETE FROM maintenance_history")
    suspend fun deleteAllMaintenanceHistories()

    @Query("DELETE FROM mileage_history")
    suspend fun deleteAllMileageHistories()

    @Query("DELETE FROM car_maintenance_settings")
    suspend fun deleteAllMaintenanceSettings()

    @Query("DELETE FROM maintenance_types")
    suspend fun deleteAllMaintenanceTypes()

    @Query("DELETE FROM cars")
    suspend fun deleteAllCars()
}