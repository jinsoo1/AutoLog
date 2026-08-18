package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.buildMonthlyReportNotice
import com.jsworld.android.autolog.domain.model.nextMonthlyReportTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class MonthlyReportNoticeTest {

    private val july = YearMonth.of(2026, 7)

    private fun expense(
        month: YearMonth = july,
        fuel: Long = 0,
        maint: Long = 0,
        care: Long = 0,
        missing: Int = 0
    ) = MonthlyExpense(
        month = month,
        fuelCost = fuel,
        maintenanceCost = maint,
        careCost = care,
        missingCostCount = missing,
        drivenKm = null
    )

    @Test
    fun `지난달 기록이 없으면 알리지 않는다`() {
        assertNull(buildMonthlyReportNotice(july, emptyList()))
        assertNull(
            buildMonthlyReportNotice(
                july,
                listOf("아반떼" to listOf(expense(month = YearMonth.of(2026, 6), fuel = 10_000)))
            )
        )
    }

    @Test
    fun `합계 0원에 미입력도 없으면 소음이다 - 알리지 않는다`() {
        assertNull(
            buildMonthlyReportNotice(july, listOf("아반떼" to listOf(expense())))
        )
    }

    @Test
    fun `합계 0원이어도 금액 미입력 기록이 있으면 알린다`() {
        val n = buildMonthlyReportNotice(
            july,
            listOf("아반떼" to listOf(expense(missing = 2)))
        )!!
        assertEquals(0L, n.total)
        assertEquals(2, n.missingCostCount)
    }

    @Test
    fun `한 대면 차량별 줄 없이 합계만`() {
        val n = buildMonthlyReportNotice(
            july,
            listOf("아반떼" to listOf(expense(fuel = 62_814, care = 15_000)))
        )!!
        assertEquals(77_814L, n.total)
        assertTrue(n.lines.isEmpty())
    }

    @Test
    fun `여러 대면 합계 + 차량별 줄`() {
        val n = buildMonthlyReportNotice(
            july,
            listOf(
                "c클래스" to listOf(expense(fuel = 100_000, maint = 170_000)),
                "아반떼" to listOf(expense(fuel = 50_000)),
                // 지난달 기록이 없는 차는 줄에도 나오지 않는다
                "모닝" to listOf(expense(month = YearMonth.of(2026, 6), fuel = 9_999))
            )
        )!!
        assertEquals(320_000L, n.total)
        assertEquals(listOf("c클래스" to 270_000L, "아반떼" to 50_000L), n.lines.map { it.carName to it.total })
    }

    /* ── 예약 시각 ── */

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0): ZonedDateTime =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, ZoneId.of("Asia/Seoul"))

    @Test
    fun `다음 실행은 돌아오는 1일 9시`() {
        // 8월 중순 → 9월 1일
        assertEquals(at(2026, 9, 1, 9), nextMonthlyReportTime(at(2026, 8, 16, 10), 9))
        // 1일 9시 직전 → 오늘 9시
        assertEquals(at(2026, 9, 1, 9), nextMonthlyReportTime(at(2026, 9, 1, 8, 59), 9))
        // 1일 9시 정각(이미 도달) → 다음 달 1일
        assertEquals(at(2026, 10, 1, 9), nextMonthlyReportTime(at(2026, 9, 1, 9), 9))
        // 12월 → 해를 넘겨 1월 1일
        assertEquals(at(2027, 1, 1, 9), nextMonthlyReportTime(at(2026, 12, 15, 12), 9))
    }
}
