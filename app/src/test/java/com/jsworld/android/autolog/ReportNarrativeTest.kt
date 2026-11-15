package com.jsworld.android.autolog

import com.jsworld.android.autolog.TestStrings.ko
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.NarrativeTone
import com.jsworld.android.autolog.domain.model.buildReportNarrative
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ReportNarrativeTest {

    private fun expense(
        month: YearMonth = YearMonth.of(2026, 8),
        total: Long = 0,
        drivenKm: Int? = null
    ) = MonthlyExpense(
        month = month,
        fuelCost = total,
        maintenanceCost = 0,
        careCost = 0,
        missingCostCount = 0,
        drivenKm = drivenKm
    )

    @Test
    fun `내러티브 우선순위 - 초과가 급증보다 먼저다`() {
        val n = buildReportNarrative(
            current = expense(total = 500_000),
            previous = expense(total = 100_000),
            overdueCount = 2,
            soonCount = 1
        )
        assertEquals(NarrativeTone.WARNING, n.tone)
        assertTrue(n.subtitle.ko().contains("2개"))
    }

    @Test
    fun `급증 - 10만원 이상이면서 1_5배 이상일 때만`() {
        val spike = buildReportNarrative(expense(total = 300_000), expense(total = 150_000), 0, 0)
        assertEquals(NarrativeTone.SPIKE, spike.tone)

        // 1.5배지만 증가액이 10만원 미만 → 평온
        val small = buildReportNarrative(expense(total = 90_000), expense(total = 50_000), 0, 0)
        assertEquals(NarrativeTone.CALM, small.tone)
    }

    @Test
    fun `기록 없는 달과 평온한 달을 구분한다`() {
        assertEquals(NarrativeTone.EMPTY, buildReportNarrative(expense(total = 0), null, 0, 0).tone)
        assertEquals(NarrativeTone.CALM, buildReportNarrative(expense(total = 50_000), null, 0, 0).tone)
    }
}
