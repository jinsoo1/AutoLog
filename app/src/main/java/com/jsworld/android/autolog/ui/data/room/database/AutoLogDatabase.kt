package com.jsworld.android.autolog.ui.data.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsworld.android.autolog.ui.data.room.dao.CarDao
import com.jsworld.android.autolog.ui.data.room.dao.CarExportDao
import com.jsworld.android.autolog.ui.data.room.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceFullDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.ui.data.room.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.ui.data.room.dao.MileageHistoryDao
import com.jsworld.android.autolog.ui.data.room.entity.CarEntity
import com.jsworld.android.autolog.ui.data.room.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.ui.data.room.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.ui.data.room.entity.MileageHistoryEntity

@Database(
    entities = [
        CarEntity::class,
        MaintenanceTypeEntity::class,
        CarMaintenanceSettingEntity::class,
        MaintenanceHistoryEntity::class,
        MileageHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AutoLogDatabase : RoomDatabase() {

    abstract fun carDao(): CarDao
    abstract fun maintenanceTypeDao(): MaintenanceTypeDao
    abstract fun carMaintenanceSettingDao(): CarMaintenanceSettingDao
    abstract fun maintenanceHistoryDao(): MaintenanceHistoryDao
    abstract fun maintenanceFullDao(): MaintenanceFullDao
    abstract fun mileageHistoryDao(): MileageHistoryDao
    abstract fun carExportDao(): CarExportDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1) cars 테이블에 lastMileageUpdatedAt 추가
        db.execSQL(
            """
            ALTER TABLE cars ADD COLUMN lastMileageUpdatedAt INTEGER
            """.trimIndent()
        )

        // 2) 기존 스키마에 이미 isActive가 있을 가능성이 높으므로
        // car_maintenance_settings에 isActive 추가는 하지 않음

        // 3) car_maintenance_settings 복합 unique index 추가
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_car_maintenance_settings_carId_maintenanceTypeId
            ON car_maintenance_settings(carId, maintenanceTypeId)
            """.trimIndent()
        )

        // 4) maintenance_types 이름 unique index 추가
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_maintenance_types_name
            ON maintenance_types(name)
            """.trimIndent()
        )

        // 5) mileage_history 테이블 생성
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS mileage_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                carId INTEGER NOT NULL,
                mileage INTEGER NOT NULL,
                recordedAt INTEGER NOT NULL,
                memo TEXT,
                FOREIGN KEY(carId) REFERENCES cars(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_mileage_history_carId
            ON mileage_history(carId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_mileage_history_carId_recordedAt
            ON mileage_history(carId, recordedAt)
            """.trimIndent()
        )

        // 6) 기존 cars.mileage 를 mileage_history에 초기 이력으로 백필
        db.execSQL(
            """
            INSERT INTO mileage_history (carId, mileage, recordedAt, memo)
            SELECT id, mileage, 0, 'Migrated from cars.mileage'
            FROM cars
            WHERE mileage > 0
            """.trimIndent()
        )

        // 7) 기존 mileage 가 있는 차량은 lastMileageUpdatedAt 을 0으로 채움
        db.execSQL(
            """
            UPDATE cars
            SET lastMileageUpdatedAt = CASE
                WHEN mileage > 0 THEN 0
                ELSE NULL
            END
            """.trimIndent()
        )
    }
}