package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceRecordRow
import com.jsworld.android.autolog.data.local.entity.MaintenanceCostRow
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MileagePointRow
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

    /**
     * 차량의 모든 정비 기록을 항목 이름과 함께 최신순으로 조회한다.
     * 정비 탭의 통합 타임라인용 — 항목별로 흩어진 기록을 한 화면에서 보기 위한 쿼리다.
     */
    @Query("""
        SELECT h.id            AS historyId,
               h.settingId     AS settingId,
               s.maintenanceTypeId AS typeId,
               t.name          AS typeName,
               h.serviceDate   AS serviceDate,
               h.serviceMileage AS serviceMileage,
               h.place         AS place,
               h.cost          AS cost,
               h.memo          AS memo,
               CASE WHEN s.intervalKm IS NULL AND s.intervalMonths IS NULL
                     AND t.defaultIntervalKm IS NULL AND t.defaultIntervalMonths IS NULL
                    THEN 1 ELSE 0 END AS isRepair
        FROM maintenance_history h
        JOIN car_maintenance_settings s ON s.id = h.settingId
        JOIN maintenance_types t ON t.id = s.maintenanceTypeId
        WHERE s.carId = :carId
        ORDER BY h.serviceDate DESC, h.serviceMileage DESC, h.id DESC
    """)
    fun observeCarRecords(carId: Long): Flow<List<CarMaintenanceRecordRow>>

    /**
     * 리포트용 — 월·항목명·금액. 카테고리(정비·수리/세차) 분류는 이름 기반이라
     * 코틀린(isCareItemName)에서 하고, 여기서는 원천 행만 낸다.
     * 날짜 없는 기록은 어느 달에도 넣을 수 없어 제외한다.
     */
    @Query("""
        SELECT substr(h.serviceDate, 1, 7) AS month,
               t.name AS typeName,
               h.cost AS cost
        FROM maintenance_history h
        JOIN car_maintenance_settings s ON s.id = h.settingId
        JOIN maintenance_types t ON t.id = s.maintenanceTypeId
        WHERE s.carId = :carId AND h.serviceDate IS NOT NULL
    """)
    fun observeMonthlyCostRows(carId: Long): Flow<List<MaintenanceCostRow>>

    /** 리포트용 — 주행거리 관측점(정비 기록의 날짜·누적 km) */
    @Query("""
        SELECT h.serviceDate AS date, h.serviceMileage AS mileage
        FROM maintenance_history h
        JOIN car_maintenance_settings s ON s.id = h.settingId
        WHERE s.carId = :carId
          AND h.serviceDate IS NOT NULL AND h.serviceMileage IS NOT NULL
    """)
    fun observeMileagePoints(carId: Long): Flow<List<MileagePointRow>>
}
