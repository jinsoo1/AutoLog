package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceHistoryDao {

    @Query("""
        SELECT * FROM maintenance_history 
        WHERE settingId = :settingId 
        ORDER BY serviceMileage DESC
    """)
    fun getHistoryForSetting(settingId: Long): Flow<List<MaintenanceHistoryEntity>>

    @Insert
    suspend fun insertHistory(history: MaintenanceHistoryEntity): Long

    @Update
    suspend fun updateHistory(history: MaintenanceHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: MaintenanceHistoryEntity)

    @Query("""
        SELECT MAX(h.serviceMileage)
        FROM maintenance_history h
        JOIN car_maintenance_settings s ON s.id = h.settingId
        WHERE s.carId = :carId
          AND h.serviceMileage IS NOT NULL
    """)
    suspend fun getMaxServiceMileageForCar(carId: Long): Int?

    @Query("""
        SELECT *
        FROM maintenance_history
        WHERE settingId = :settingId
        ORDER BY 
            serviceDate DESC,
            serviceMileage DESC,
            id DESC
        LIMIT 1
    """)
    fun observeLastHistory(settingId: Long): Flow<MaintenanceHistoryEntity?>

    // 전체 내역(최신순)
    @Query("""
        SELECT *
        FROM maintenance_history
        WHERE settingId = :settingId
        ORDER BY 
            serviceDate DESC,
            serviceMileage DESC,
            id DESC
    """)
    fun observeHistories(settingId: Long): Flow<List<MaintenanceHistoryEntity>>

    @Query("""
        SELECT MAX(h.serviceMileage)
        FROM maintenance_history h
        JOIN car_maintenance_settings s ON s.id = h.settingId
        WHERE s.carId = :carId
    """)
    fun observeMaxServiceMileageForCar(carId: Long): Flow<Int?>

    @Query("SELECT * FROM maintenance_history WHERE id = :id")
    fun getHistoryById(id: Long): Flow<MaintenanceHistoryEntity?>

    @Query("""
        SELECT * FROM maintenance_history
        WHERE settingId = :settingId
        ORDER BY serviceDate ASC, serviceMileage ASC, id ASC
    """)
    fun getHistoriesForSetting(settingId: Long): Flow<List<MaintenanceHistoryEntity>>

    @Query("DELETE FROM maintenance_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
}
