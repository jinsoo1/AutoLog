package com.jsworld.android.autolog.ui.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import com.jsworld.android.autolog.ui.data.room.entity.MileageHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.SettingWithHistoryEntity

@Dao
interface CarExportDao {

    /**
     * 엑셀로 출력할 차량 1대 정보
     */
    @Query("""
        SELECT *
        FROM cars
        WHERE id = :carId
        LIMIT 1
    """)
    suspend fun getCarForExport(carId: Long): CarEntity?

    /**
     * 차량에 등록된 정비 항목 + 정비 타입 + 정비 이력
     *
     * SettingWithHistoryEntity 내부에 @Relation이 있으므로
     * @Transaction을 붙이는 것이 좋습니다.
     */
    @Transaction
    @Query("""
        SELECT *
        FROM car_maintenance_settings
        WHERE carId = :carId
          AND isActive = 1
        ORDER BY id ASC
    """)
    suspend fun getSettingsWithHistoryForExport(
        carId: Long
    ): List<SettingWithHistoryEntity>

    /**
     * 차량의 주행거리 히스토리
     */
    @Query("""
        SELECT *
        FROM mileage_history
        WHERE carId = :carId
        ORDER BY recordedAt DESC
    """)
    suspend fun getMileageHistoriesForExport(
        carId: Long
    ): List<MileageHistoryEntity>
}