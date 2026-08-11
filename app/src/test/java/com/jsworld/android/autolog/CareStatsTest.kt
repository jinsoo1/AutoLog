package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.buildCareOverview
import com.jsworld.android.autolog.domain.model.careCounts
import com.jsworld.android.autolog.domain.model.upkeepLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CareStatsTest {

    private val today = LocalDate.of(2026, 8, 11)

    private fun record(name: String, date: String?, cost: Int? = null) = CarMaintenanceRecord(
        historyId = 0, settingId = 0, typeId = 0, typeName = name,
        serviceDate = date, serviceMileage = null, place = null,
        cost = cost, memo = null, isRepair = true
    )

    @Test
    fun `세차 기록이 없으면 경과일도 평균도 없다`() {
        val o = buildCareOverview(listOf(record("코팅", "2026-07-01")), today)
        assertNull(o.daysSinceWash)
        assertNull(o.averageIntervalDays)
        assertFalse(o.isDue)
    }

    @Test
    fun `경과일은 마지막 세차 기준`() {
        val o = buildCareOverview(
            listOf(record("세차", "2026-07-30"), record("세차", "2026-07-10")),
            today
        )
        assertEquals(12, o.daysSinceWash)
        assertNull(o.averageIntervalDays) // 2회 — 평균은 3회부터
    }

    @Test
    fun `평균 간격은 3회부터, 넘기면 때가 됐다고 본다`() {
        // 6/10 → 6/26 → 7/12 → 7/28: 48일 / 3구간 = 16일. 오늘까지 14일 → 아직
        val notDue = buildCareOverview(
            listOf(
                record("세차", "2026-06-10"), record("세차", "2026-06-26"),
                record("세차", "2026-07-12"), record("세차", "2026-07-28")
            ),
            today
        )
        assertEquals(16, notDue.averageIntervalDays)
        assertFalse(notDue.isDue)

        // 마지막이 7/20이면 22일 경과 >= 16 → 때가 됨
        val due = buildCareOverview(
            listOf(
                record("세차", "2026-06-02"), record("세차", "2026-06-18"),
                record("세차", "2026-07-04"), record("세차", "2026-07-20")
            ),
            today
        )
        assertTrue(due.isDue)
    }

    @Test
    fun `횟수와 올해 비용 집계`() {
        val c = careCounts(
            listOf(
                record("세차", "2026-08-05", 15_000),
                record("세차", "2026-08-01", 8_000),
                record("코팅", "2026-03-02", 120_000),
                record("세차", "2025-12-30", 9_000) // 작년 — 제외
            ),
            today
        )
        assertEquals(2, c.monthCount)
        assertEquals(3, c.yearCount)
        assertEquals(143_000L, c.yearCost)
    }

    @Test
    fun `유지 관리 - 세차 외 항목의 마지막 시점만, 가까운 순`() {
        val lines = upkeepLines(
            listOf(
                record("세차", "2026-08-05"),
                record("코팅", "2026-06-27"),
                record("코팅", "2026-03-01"),
                record("왁스", "2026-07-22")
            ),
            today
        )
        assertEquals(listOf("왁스" to 20, "코팅" to 45), lines)
    }
}
