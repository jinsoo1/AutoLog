package com.jsworld.android.autolog.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsworld.android.autolog.data.local.dao.BackupDao
import com.jsworld.android.autolog.data.local.dao.CarDao
import com.jsworld.android.autolog.data.local.dao.CarExportDao
import com.jsworld.android.autolog.data.local.dao.CarScheduleDao
import com.jsworld.android.autolog.data.local.dao.CarMaintenanceSettingDao
import com.jsworld.android.autolog.data.local.dao.CareDao
import com.jsworld.android.autolog.data.local.dao.FuelRecordDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceFullDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceHistoryDao
import com.jsworld.android.autolog.data.local.dao.MaintenanceTypeDao
import com.jsworld.android.autolog.data.local.dao.MileageHistoryDao
import com.jsworld.android.autolog.data.local.entity.CarEntity
import com.jsworld.android.autolog.data.local.entity.CarMaintenanceSettingEntity
import com.jsworld.android.autolog.data.local.entity.CarScheduleEntity
import com.jsworld.android.autolog.data.local.entity.CareItemEntity
import com.jsworld.android.autolog.data.local.entity.CareRecordEntity
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
        FuelRecordEntity::class,
        CareItemEntity::class,
        CareRecordEntity::class,
        CarScheduleEntity::class
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
    abstract fun carScheduleDao(): CarScheduleDao
    abstract fun fuelRecordDao(): FuelRecordDao
    abstract fun careDao(): CareDao

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

/**
 * 세차·관리를 정비 시스템에서 분리 — 전용 테이블 신설 + 기존 기록 이관.
 *
 * 정비 테이블에 세차를 두면 모든 정비 쿼리가 "세차 제외" 필터를 기억해야 하고,
 * 하나만 빼먹어도 세차가 정비 경고에 다시 섞인다. 테이블을 나누면 그 규칙 자체가
 * 사라진다. 세차 항목은 차량별 단일 테이블(care_items)로 두고, "세차 N회마다"
 * 주기(intervalWashCount)와 방식(method) 등 세차 전용 필드를 갖는다.
 *
 * 이관: 이름 규칙(세차/코팅/왁스/광택/세정)으로 기존 세차 항목·기록을 골라
 * 새 테이블로 옮기고 정비 테이블에서 지운다. 트랜잭션 안에서 돌므로
 * 실패 시 전체 롤백된다.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {

    /**
     * 옛 이름을 새 세차 항목 이름으로 맞춘다.
     * "실내/외 세차(관리)"를 그대로 두면 기본 '세차'와 중복돼 목록이 지저분해지고
     * 과거 기록이 세차 카운터에 잡히지 않는다.
     *
     * ⚠️ DefaultCareItems.normalizeLegacyName 과 같은 규칙을 유지할 것.
     */
    private val normalizedName = """
        CASE
            WHEN REPLACE(t.name, ' ', '') LIKE '%실내/외세차%' THEN '세차'
            WHEN REPLACE(t.name, ' ', '') LIKE '%코팅/왁스%' THEN '왁스코팅'
            ELSE t.name
        END
    """.trimIndent()

    // 이름 규칙 — isCareItemName 과 같은 조건. 공백이 섞여 있어도 잡히게 한다.
    private val careNameCondition = """
        (REPLACE(t.name, ' ', '') LIKE '%세차%'
         OR REPLACE(t.name, ' ', '') LIKE '%코팅%'
         OR REPLACE(t.name, ' ', '') LIKE '%왁스%'
         OR REPLACE(t.name, ' ', '') LIKE '%광택%'
         OR REPLACE(t.name, ' ', '') LIKE '%세정%')
    """.trimIndent()

    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) 테이블 생성 — SQL 은 schemas/4.json 의 createSql 을 그대로 옮긴 것.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `care_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`carId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`intervalDays` INTEGER, " +
                "`intervalWashCount` INTEGER, " +
                "`isActive` INTEGER NOT NULL, " +
                "FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_items_carId` ON `care_items` (`carId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_care_items_carId_name` " +
                "ON `care_items` (`carId`, `name`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `care_records` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`careItemId` INTEGER NOT NULL, " +
                "`performedAt` TEXT, " +
                "`cost` INTEGER, " +
                "`method` TEXT, " +
                "`place` TEXT, " +
                "`memo` TEXT, " +
                "FOREIGN KEY(`careItemId`) REFERENCES `care_items`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_care_records_careItemId` " +
                "ON `care_records` (`careItemId`)"
        )

        // 2) 세차 항목 이관 — 같은 차에 같은 이름이 중복돼 있어도 하나로 합친다.
        db.execSQL(
            """
            INSERT INTO care_items (carId, name, intervalDays, intervalWashCount, isActive)
            SELECT s.carId, $normalizedName AS careName,
                   -- 옛 정비 주기는 개월 단위였다 → 일로 환산(1개월 = 30일)
                   MAX(s.intervalMonths) * 30, NULL, MAX(s.isActive)
            FROM car_maintenance_settings s
            JOIN maintenance_types t ON t.id = s.maintenanceTypeId
            WHERE $careNameCondition
            GROUP BY s.carId, careName
            """.trimIndent()
        )

        // 3) 세차 기록 이관
        db.execSQL(
            """
            INSERT INTO care_records (careItemId, performedAt, cost, method, place, memo)
            SELECT ci.id, h.serviceDate, h.cost, NULL, h.place, h.memo
            FROM maintenance_history h
            JOIN car_maintenance_settings s ON s.id = h.settingId
            JOIN maintenance_types t ON t.id = s.maintenanceTypeId
            JOIN care_items ci ON ci.carId = s.carId AND ci.name = $normalizedName
            WHERE $careNameCondition
            """.trimIndent()
        )

        // 4) 정비 테이블에서 세차 제거 — 기록 → 설정 → 타입 순서(참조 방향의 역순)
        db.execSQL(
            """
            DELETE FROM maintenance_history
            WHERE settingId IN (
                SELECT s.id FROM car_maintenance_settings s
                JOIN maintenance_types t ON t.id = s.maintenanceTypeId
                WHERE $careNameCondition
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            DELETE FROM car_maintenance_settings
            WHERE maintenanceTypeId IN (
                SELECT t.id FROM maintenance_types t WHERE $careNameCondition
            )
            """.trimIndent()
        )
        db.execSQL(
            "DELETE FROM maintenance_types WHERE id IN " +
                "(SELECT t.id FROM maintenance_types t WHERE $careNameCondition)"
        )
    }
}

/**
 * v5 — 날짜 기반 일정(car_schedules).
 *
 * 새 테이블 하나만 만든다. 기존 데이터는 건드리지 않으므로 되돌릴 것도 없다.
 * SQL 은 schemas/5.json 의 createSql 과 같아야 한다(Room 검증 대상).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `car_schedules` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`carId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`dueDate` TEXT NOT NULL, " +
                "`repeatMonths` INTEGER, " +
                "`memo` TEXT, " +
                "FOREIGN KEY(`carId`) REFERENCES `cars`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_car_schedules_carId` " +
                "ON `car_schedules` (`carId`)"
        )
    }
}
