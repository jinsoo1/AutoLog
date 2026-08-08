package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.AutoLogBackup
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 백업 파일 호환성.
 *
 * 사용자가 오래 전에 저장해 둔 백업을 새 버전 앱에서 복원할 수 있어야 한다.
 * DB 버전이 2 → 3 으로 오르며 `fuelRecords` 가 추가됐으므로,
 * **그 필드가 없던 백업(1.0.8 이전)** 이 그대로 복원되는지 여기서 고정한다.
 */
class BackupCompatibilityTest {

    /** BackupRepository 와 같은 설정. */
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    /** 1.0.8 시점(DB 버전 2)의 백업 JSON — fuelRecords 키가 없다. */
    private val legacyBackupJson = """
        {
            "backupVersion": 1,
            "databaseVersion": 2,
            "createdAt": 1750000000000,
            "cars": [
                {
                    "id": 1,
                    "name": "그랜저",
                    "plate": "12가1234",
                    "year": "2021",
                    "mileage": 38950,
                    "fuelType": "가솔린",
                    "notes": null,
                    "isPrimary": true,
                    "lastMileageUpdatedAt": 1749000000000
                }
            ],
            "maintenanceTypes": [
                {
                    "id": 10,
                    "name": "엔진오일",
                    "defaultIntervalKm": 10000,
                    "defaultIntervalMonths": 12
                }
            ],
            "maintenanceSettings": [
                {
                    "id": 100,
                    "carId": 1,
                    "maintenanceTypeId": 10,
                    "intervalKm": null,
                    "intervalMonths": null,
                    "isActive": true
                }
            ],
            "maintenanceHistories": [
                {
                    "id": 1000,
                    "settingId": 100,
                    "serviceDate": "2026-05-03",
                    "serviceMileage": 28600,
                    "place": "오일뱅크",
                    "cost": 92000,
                    "memo": null
                }
            ],
            "mileageHistories": [
                {
                    "id": 2000,
                    "carId": 1,
                    "mileage": 38950,
                    "recordedAt": 1749000000000,
                    "memo": null
                }
            ]
        }
    """.trimIndent()

    @Test
    fun `주유 기록이 없던 구버전 백업도 그대로 파싱된다`() {
        val backup = json.decodeFromString<AutoLogBackup>(legacyBackupJson)

        assertEquals(1, backup.backupVersion)
        assertEquals(2, backup.databaseVersion)
        assertEquals(1, backup.cars.size)
        assertEquals("그랜저", backup.cars.first().name)
        assertEquals(1, backup.maintenanceHistories.size)
        assertEquals(1, backup.mileageHistories.size)

        // 핵심: 없던 필드는 기본값(빈 목록)으로 채워진다.
        assertTrue(backup.fuelRecords.isEmpty())
    }

    @Test
    fun `구버전 백업의 backupVersion 이 현재 값과 같아 검증을 통과한다`() {
        // CURRENT_BACKUP_VERSION 을 올리면 과거 백업이 거부되므로,
        // 필드 추가(하위 호환)에서는 올리지 않아야 한다.
        val backup = json.decodeFromString<AutoLogBackup>(legacyBackupJson)
        assertEquals(AutoLogBackup.CURRENT_BACKUP_VERSION, backup.backupVersion)
    }

    @Test
    fun `새 백업은 fuelRecords 키를 항상 써서 내보낸다`() {
        // encodeDefaults = true 이므로 주유 기록이 없어도 키가 남는다.
        val backup = json.decodeFromString<AutoLogBackup>(legacyBackupJson)
        val encoded = json.encodeToString(backup)
        assertTrue(encoded.contains("\"fuelRecords\""))
    }

    @Test
    fun `모르는 키가 있어도 파싱이 깨지지 않는다`() {
        // 반대 방향(새 백업을 구버전 앱에서 여는 경우)도 같은 설정이라
        // ignoreUnknownKeys 로 견딘다는 것을 확인해 둔다.
        val withUnknown = legacyBackupJson.replaceFirst(
            "\"backupVersion\": 1,",
            "\"backupVersion\": 1,\n    \"futureFieldFromNewerApp\": [1, 2, 3],"
        )
        val backup = json.decodeFromString<AutoLogBackup>(withUnknown)
        assertEquals("그랜저", backup.cars.first().name)
    }
}
