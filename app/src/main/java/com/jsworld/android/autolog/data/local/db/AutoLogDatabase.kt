package com.jsworld.android.autolog.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsworld.android.autolog.data.local.dao.BackupDao
import com.jsworld.android.autolog.data.local.dao.CarDao
import com.jsworld.android.autolog.data.local.dao.CarExportDao
import com.jsworld.android.autolog.data.local.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.data.local.dao.FuelRecordDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceFullDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.data.local.dao.MileageHistoryDao
import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.FuelRecordEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceHistoryEntity
import com.jsworld.android.autolog.data.local.entity.MaintenanceTypeEntity
import com.jsworld.android.autolog.data.local.entity.MileageHistoryEntity
import com.jsworld.android.autolog.data.repository.BackupRepository.Companion.DATABASE_VERSION

@Database(
    entities = [
        CarEntity::class,
        MaintenanceTypeEntity::class,
        CarMaintenanceSettingEntity::class,
        MaintenanceHistoryEntity::class,
        MileageHistoryEntity::class,
        FuelRecordEntity::class
    ],
    version = DATABASE_VERSION,
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
    abstract fun fuelRecordDao(): FuelRecordDao

    abstract fun backupDao(): BackupDao
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
/**
 * 주유(충전) 기록 테이블 추가.
 *
 * 기존 테이블은 건드리지 않는 순수 추가 마이그레이션이라 데이터 유실 위험이 없다.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQL 은 app/schemas/.../3.json 의 createSql 을 그대로 옮긴 것이다.
        // 직접 쓰면 컬럼 타입·FK 옵션이 미묘하게 달라져 실행 시
        // "Migration didn't properly handle" 크래시가 나기 쉽다.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fuel_records` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`carId` INTEGER NOT NULL, " +
                "`filledAt` TEXT NOT NULL, " +
                "`mileage` INTEGER, " +
                "`amount` INTEGER, " +
                "`quantity` REAL, " +
                "`unitPrice` INTEGER, " +
                "`unit` TEXT NOT NULL, " +
                "`station` TEXT, " +
                "`memo` TEXT, " +
                "`photoPath` TEXT, " +
                "FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fuel_records_carId` " +
                "ON `fuel_records` (`carId`)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fuel_records_carId_filledAt` " +
                "ON `fuel_records` (`carId`, `filledAt`)"
        )
    }
}
