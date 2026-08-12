package com.jsworld.android.autolog

import com.jsworld.android.autolog.data.repository.AutoLogBackup
import com.jsworld.android.autolog.data.repository.CarBackup
import com.jsworld.android.autolog.data.repository.CareItemBackup
import com.jsworld.android.autolog.data.repository.MaintenanceHistoryBackup
import com.jsworld.android.autolog.data.repository.MaintenanceSettingBackup
import com.jsworld.android.autolog.data.repository.MaintenanceTypeBackup
import com.jsworld.android.autolog.data.repository.withLegacyCareConverted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DB v4 이전 백업(세차가 정비 목록에 섞여 있음)의 복원 호환.
 * 이 변환이 깨지면 구버전 백업을 복원한 사용자의 세차 기록이 사라지거나
 * 정비 타임라인에 다시 섞인다.
 */
class CareLegacyBackupTest {

    private fun legacyBackup(): AutoLogBackup {
        val car = CarBackup(
            id = 1, name = "아반떼", plate = "12가3456", year = "2020",
            mileage = 50_000, fuelType = "가솔린", notes = null,
            isPrimary = true, lastMileageUpdatedAt = null
        )
        val types = listOf(
            MaintenanceTypeBackup(1, "엔진오일", 10_000, 6),
            MaintenanceTypeBackup(2, "실내/외 세차(관리)", null, null),
            MaintenanceTypeBackup(3, "코팅/왁스(관리)", null, null)
        )
        val settings = listOf(
            MaintenanceSettingBackup(10, 1, 1, null, null, true),
            MaintenanceSettingBackup(11, 1, 2, null, 1, true),   // 세차, 1개월 주기
            MaintenanceSettingBackup(12, 1, 3, null, null, false) // 코팅, 꺼짐
        )
        val histories = listOf(
            MaintenanceHistoryBackup(100, 10, "2026-08-01", 49_000, "정비소", 89_000, null),
            MaintenanceHistoryBackup(101, 11, "2026-08-05", null, "OO세차장", 15_000, "셀프"),
            MaintenanceHistoryBackup(102, 12, "2026-07-02", null, null, 120_000, null)
        )
        return AutoLogBackup(
            databaseVersion = 3,
            createdAt = 0L,
            cars = listOf(car),
            maintenanceTypes = types,
            maintenanceSettings = settings,
            maintenanceHistories = histories,
            mileageHistories = emptyList()
        )
    }

    @Test
    fun `세차 항목과 기록이 새 구조로 옮겨지고 정비 목록에서 빠진다`() {
        val converted = legacyBackup().withLegacyCareConverted()

        // 정비 쪽에는 엔진오일만 남는다
        assertEquals(listOf("엔진오일"), converted.maintenanceTypes.map { it.name })
        assertEquals(1, converted.maintenanceSettings.size)
        assertEquals(1, converted.maintenanceHistories.size)

        // 세차 쪽으로 항목 2개, 기록 2건이 옮겨진다.
        // 옛 이름은 새 항목 이름으로 정규화된다 — 그대로 두면 기본 '세차'와 중복되고
        // 과거 기록이 세차 카운터에 잡히지 않는다.
        assertEquals(
            listOf("세차", "왁스코팅"),
            converted.careItems.map { it.name }
        )
        assertEquals(2, converted.careRecords.size)

        // 주기·활성 상태·기록 내용이 보존된다
        val wash = converted.careItems.first { it.name == "세차" }
        assertEquals(30, wash.intervalDays) // 옛 1개월 주기 → 30일
        assertTrue(wash.isActive)

        val washRecord = converted.careRecords.first { it.careItemId == wash.id }
        assertEquals("2026-08-05", washRecord.performedAt)
        assertEquals(15_000, washRecord.cost)
        assertEquals("OO세차장", washRecord.place)
        assertEquals("셀프", washRecord.memo)
    }

    @Test
    fun `옛 세차 이름과 새 세차 이름이 한 항목으로 병합된다`() {
        // 1.2.0 에서 "실내/외 세차(관리)"로 기록하다가 세차 허브에서 "세차"로도
        // 기록한 사용자 — 두 이름의 기록이 하나의 세차 항목으로 합쳐져야 한다.
        val base = legacyBackup()
        val mixed = base.copy(
            maintenanceTypes = base.maintenanceTypes + MaintenanceTypeBackup(4, "세차", null, null),
            maintenanceSettings = base.maintenanceSettings +
                MaintenanceSettingBackup(13, 1, 4, null, null, true),
            maintenanceHistories = base.maintenanceHistories +
                MaintenanceHistoryBackup(103, 13, "2026-08-09", null, null, 12_000, null)
        )

        val converted = mixed.withLegacyCareConverted()
        assertEquals(listOf("세차", "왁스코팅"), converted.careItems.map { it.name })

        val washId = converted.careItems.first { it.name == "세차" }.id
        assertEquals(2, converted.careRecords.count { it.careItemId == washId })
    }

    @Test
    fun `기록과 항목의 참조가 일치한다`() {
        val converted = legacyBackup().withLegacyCareConverted()
        val itemIds = converted.careItems.map { it.id }.toSet()
        converted.careRecords.forEach { assertTrue(it.careItemId in itemIds) }
    }

    @Test
    fun `새 백업(careItems 있음)은 변환하지 않는다`() {
        val newBackup = legacyBackup().copy(
            careItems = listOf(CareItemBackup(1, 1, "세차", null, 3, true))
        )
        val converted = newBackup.withLegacyCareConverted()
        assertEquals(newBackup, converted)
    }

    @Test
    fun `세차가 없는 백업은 그대로 돌려준다`() {
        val noCare = legacyBackup().let {
            it.copy(
                maintenanceTypes = it.maintenanceTypes.filter { t -> t.name == "엔진오일" },
                maintenanceSettings = it.maintenanceSettings.filter { s -> s.id == 10L },
                maintenanceHistories = it.maintenanceHistories.filter { h -> h.settingId == 10L }
            )
        }
        assertEquals(noCare, noCare.withLegacyCareConverted())
    }
}
