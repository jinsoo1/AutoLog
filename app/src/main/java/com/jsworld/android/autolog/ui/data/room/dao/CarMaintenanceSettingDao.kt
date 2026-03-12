package com.jsworld.android.autolog.ui.data.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jsworld.android.autolog.ui.data.room.entity.CarMaintenanceSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarMaintenanceSettingDao {

    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId AND isActive = 1")
    fun getSettingsForCar(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    @Insert
    suspend fun insertSetting(setting: CarMaintenanceSettingEntity): Long

    @Update
    suspend fun updateSetting(setting: CarMaintenanceSettingEntity)

    @Delete
    suspend fun deleteSetting(setting: CarMaintenanceSettingEntity)


    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId AND isActive = 1")
    fun observeByCarId(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId AND isActive = 1")
    suspend fun getByCarIdOnce(carId: Long): List<CarMaintenanceSettingEntity>

    @Insert
    suspend fun insertAll(list: List<CarMaintenanceSettingEntity>)

    // ✅ Picker는 비활성 포함으로 읽어야 함
    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId")
    fun observeByCarIdIncludingInactive(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId")
    suspend fun getByCarIdOnceIncludingInactive(carId: Long): List<CarMaintenanceSettingEntity>

    // ✅ carId + typeId로 1건 찾기(있으면 enable/disable, 없으면 insert)
    @Query("""
        SELECT * FROM car_maintenance_settings
        WHERE carId = :carId AND maintenanceTypeId = :typeId
        LIMIT 1
    """)
    suspend fun getOneByCarIdAndTypeId(carId: Long, typeId: Long): CarMaintenanceSettingEntity?

    @Query("UPDATE car_maintenance_settings SET isActive = 0 WHERE id = :settingId")
    suspend fun disableSetting(settingId: Long)

    @Query("UPDATE car_maintenance_settings SET isActive = 1 WHERE id = :settingId")
    suspend fun enableSetting(settingId: Long)

    @Query("""
    UPDATE car_maintenance_settings
    SET isActive = 0
    WHERE carId = :carId AND maintenanceTypeId IN (:typeIds)
""")
    suspend fun disableByCarIdAndTypeIds(carId: Long, typeIds: List<Long>)


    @Query(
        """
    SELECT * FROM car_maintenance_settings
    WHERE id = :settingId AND isActive = 1
    """)
    fun observeBySettingId(settingId: Long): Flow<CarMaintenanceSettingEntity?>

    @Query("""
        UPDATE car_maintenance_settings
        SET intervalKm = :intervalKm,
            intervalMonths = :intervalMonths
        WHERE id = :settingId
    """)
    suspend fun updateIntervals(settingId: Long, intervalKm: Int?, intervalMonths: Int?)


    /**
     * 정렬 관련 쿼리
     */

    /**
     * 1) 잔여 키로수 순 (가까운 순)
     * 잔여km = (마지막정비km + 주기km) - 차량현재km
     * => 음수(초과) → 0(도래) → 작은 양수(곧 도래) 순으로 위에 옵니다.
     */
    @Query("""
    SELECT s.*
    FROM car_maintenance_settings s
    JOIN cars c ON c.id = s.carId
    JOIN maintenance_types t ON t.id = s.maintenanceTypeId
    LEFT JOIN (
        SELECT settingId, MAX(serviceMileage) AS lastMileage
        FROM maintenance_history
        GROUP BY settingId
    ) lm ON lm.settingId = s.id
    WHERE s.carId = :carId AND s.isActive = 1
    ORDER BY
        CASE
            WHEN lm.lastMileage IS NULL THEN 2147483647
            WHEN COALESCE(s.intervalKm, t.defaultIntervalKm) IS NULL THEN 2147483647
            ELSE (lm.lastMileage + COALESCE(s.intervalKm, t.defaultIntervalKm)) - c.mileage
        END ASC,
        t.name ASC
""")
    fun getSettingsForCarOrderByRemainingKm(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    /**
     * 2) 도래 날짜 순 (가까운 순)
     * 도래일 = 마지막정비일 + 주기개월
     * 정렬값 = (도래일 - 오늘) 일수
     * => 음수(초과) → 0(오늘 도래) → 작은 양수(곧 도래) 순으로 위에 옵니다.
     */
    @Query("""
    SELECT s.*
    FROM car_maintenance_settings s
    JOIN maintenance_types t ON t.id = s.maintenanceTypeId
    LEFT JOIN (
        SELECT settingId, MAX(serviceDate) AS lastDate
        FROM maintenance_history
        GROUP BY settingId
    ) ld ON ld.settingId = s.id
    WHERE s.carId = :carId AND s.isActive = 1
    ORDER BY
        CASE
            WHEN ld.lastDate IS NULL THEN 1000000000.0
            WHEN COALESCE(s.intervalMonths, t.defaultIntervalMonths) IS NULL THEN 1000000000.0
            ELSE (
                julianday(
                    date(ld.lastDate, '+' || COALESCE(s.intervalMonths, t.defaultIntervalMonths) || ' months')
                ) - julianday(date('now','localtime'))
            )
        END ASC,
        t.name ASC
""")
    fun getSettingsForCarOrderByDueDate(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    @Query("""
    SELECT s.*
    FROM car_maintenance_settings s
    JOIN cars c ON c.id = s.carId
    JOIN maintenance_types t ON t.id = s.maintenanceTypeId
    LEFT JOIN (
        SELECT settingId,
               MAX(serviceMileage) AS lastMileage,
               MAX(serviceDate)    AS lastDate
        FROM maintenance_history
        GROUP BY settingId
    ) lh ON lh.settingId = s.id
    WHERE s.carId = :carId AND s.isActive = 1
    ORDER BY
        CASE
            WHEN lh.lastMileage IS NOT NULL
                 AND COALESCE(s.intervalKm, t.defaultIntervalKm) IS NOT NULL
                 AND COALESCE(s.intervalKm, t.defaultIntervalKm) > 0
                 AND lh.lastDate IS NOT NULL
                 AND COALESCE(s.intervalMonths, t.defaultIntervalMonths) IS NOT NULL
                 AND COALESCE(s.intervalMonths, t.defaultIntervalMonths) > 0
            THEN MIN(
                (
                    ((lh.lastMileage + COALESCE(s.intervalKm, t.defaultIntervalKm)) - c.mileage) * 1.0
                    / COALESCE(s.intervalKm, t.defaultIntervalKm)
                ),
                (
                    (julianday(date(lh.lastDate, '+' || COALESCE(s.intervalMonths, t.defaultIntervalMonths) || ' months'))
                     - julianday(date('now','localtime'))) * 1.0
                    / (COALESCE(s.intervalMonths, t.defaultIntervalMonths) * 30.0)
                )
            )

            WHEN lh.lastMileage IS NOT NULL
                 AND COALESCE(s.intervalKm, t.defaultIntervalKm) IS NOT NULL
                 AND COALESCE(s.intervalKm, t.defaultIntervalKm) > 0
            THEN (
                ((lh.lastMileage + COALESCE(s.intervalKm, t.defaultIntervalKm)) - c.mileage) * 1.0
                / COALESCE(s.intervalKm, t.defaultIntervalKm)
            )

            WHEN lh.lastDate IS NOT NULL
                 AND COALESCE(s.intervalMonths, t.defaultIntervalMonths) IS NOT NULL
                 AND COALESCE(s.intervalMonths, t.defaultIntervalMonths) > 0
            THEN (
                (julianday(date(lh.lastDate, '+' || COALESCE(s.intervalMonths, t.defaultIntervalMonths) || ' months'))
                 - julianday(date('now','localtime'))) * 1.0
                / (COALESCE(s.intervalMonths, t.defaultIntervalMonths) * 30.0)
            )

            ELSE 1000000000.0
        END ASC,
        t.name ASC
""")
    fun getSettingsForCarOrderByCombined(carId: Long): Flow<List<CarMaintenanceSettingEntity>>



    // ✅ Picker 전용: 활성+비활성 모두
    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId")
    fun observeAllByCarId(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    // ✅ 화면(디테일 정렬 등) 전용: 활성만
    @Query("SELECT * FROM car_maintenance_settings WHERE carId = :carId AND isActive = 1")
    fun observeActiveByCarId(carId: Long): Flow<List<CarMaintenanceSettingEntity>>

    @Query("""
        UPDATE car_maintenance_settings
        SET isActive = :active
        WHERE id = :settingId
    """)
    suspend fun setActive(settingId: Long, active: Boolean)

    @Query("""
        SELECT * FROM car_maintenance_settings
        WHERE carId = :carId AND maintenanceTypeId = :typeId
        LIMIT 1
    """)
    suspend fun getByCarIdAndTypeIdOnce(carId: Long, typeId: Long): CarMaintenanceSettingEntity?

    // (선택) 배치로 쓰고 싶으면
    @Query("""
        UPDATE car_maintenance_settings
        SET isActive = :active
        WHERE carId = :carId AND maintenanceTypeId IN (:typeIds)
    """)
    suspend fun setActiveByCarIdAndTypeIds(carId: Long, typeIds: List<Long>, active: Boolean)

    @Query("SELECT carId FROM car_maintenance_settings WHERE id = :settingId")
    suspend fun getCarIdBySettingId(settingId: Long): Long
}