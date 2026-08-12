package com.jsworld.android.autolog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.jsworld.android.autolog.data.local.entity.SettingOptionRow
import com.jsworld.android.autolog.data.local.entity.CarWithSettings
import com.jsworld.android.autolog.data.local.entity.SettingWithTypeAndHistories
import com.jsworld.android.autolog.data.local.entity.SettingWithHistory as RoomSettingWithHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceFullDao {

    @Transaction
    @Query("SELECT * FROM cars WHERE id = :carId")
    fun getCarFullData(carId: Long): Flow<List<CarWithSettings>>

    @Transaction
    @Query("""
    SELECT * 
    FROM car_maintenance_settings 
    WHERE carId = :carId
    AND isActive = 1
    ORDER BY id ASC
    """)
    fun getSettingsWithHistory(carId: Long): Flow<List<SettingWithTypeAndHistories>>

    @Query("""
        SELECT 
            s.id AS settingId,
            t.name AS typeName,
            MAX(h.serviceDate) AS lastServiceDate,
            MAX(h.serviceMileage) AS lastServiceMileage,
            t.isCare AS isCare,
            COALESCE(s.intervalMonths, t.defaultIntervalMonths) AS intervalMonths,
            s.intervalWashCount AS intervalWashCount
        FROM car_maintenance_settings s
        JOIN maintenance_types t ON t.id = s.maintenanceTypeId
        LEFT JOIN maintenance_history h ON h.settingId = s.id
        WHERE s.carId = :carId 
        AND s.isActive = 1
        GROUP BY s.id, t.name, t.isCare, s.intervalMonths, t.defaultIntervalMonths, s.intervalWashCount
        ORDER BY t.name
    """)
    fun observeSettingOptions(carId: Long): Flow<List<SettingOptionRow>>


    @Transaction
    @Query("""
        SELECT s.*
        FROM car_maintenance_settings s
        WHERE s.carId = :carId
          AND s.isActive = 1
        ORDER BY s.id ASC
    """)
    fun getSettingsWithHistoryDefault(carId: Long): Flow<List<RoomSettingWithHistory>>

    @Transaction
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
        WHERE s.carId = :carId
          AND s.isActive = 1
        ORDER BY
            CASE
                WHEN lm.lastMileage IS NULL THEN 2147483647
                WHEN COALESCE(s.intervalKm, t.defaultIntervalKm) IS NULL THEN 2147483647
                ELSE (lm.lastMileage + COALESCE(s.intervalKm, t.defaultIntervalKm)) - c.mileage
            END ASC,
            t.name ASC
    """)
    fun getSettingsWithHistoryOrderByRemainingKm(carId: Long): Flow<List<RoomSettingWithHistory>>

    @Transaction
    @Query("""
        SELECT s.*
        FROM car_maintenance_settings s
        JOIN maintenance_types t ON t.id = s.maintenanceTypeId
        LEFT JOIN (
            SELECT settingId, MAX(serviceDate) AS lastDate
            FROM maintenance_history
            GROUP BY settingId
        ) ld ON ld.settingId = s.id
        WHERE s.carId = :carId
          AND s.isActive = 1
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
    fun getSettingsWithHistoryOrderByDueDate(carId: Long): Flow<List<RoomSettingWithHistory>>

    @Transaction
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
        WHERE s.carId = :carId
          AND s.isActive = 1
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
    fun getSettingsWithHistoryOrderByCombined(carId: Long): Flow<List<RoomSettingWithHistory>>


}