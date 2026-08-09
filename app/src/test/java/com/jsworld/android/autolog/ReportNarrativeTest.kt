package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.NarrativeTone
import com.jsworld.android.autolog.domain.model.buildReportNarrative
import com.jsworld.android.autolog.domain.model.distanceLadderText
import com.jsworld.android.autolog.domain.model.earthLapsText
import com.jsworld.android.autolog.domain.model.personalRecordText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertTrue(n.subtitle.contains("2개"))
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

    @Test
    fun `지구 바퀴 - 한 바퀴 전에는 퍼센트, 넘으면 바퀴 수`() {
        assertTrue(earthLapsText(20_000)!!.contains("한 바퀴의 50%"))
        assertTrue(earthLapsText(72_000)!!.contains("1.8바퀴째"))
        assertNull(earthLapsText(0))
    }

    @Test
    fun `자기 기록 - 과거 3달 이상 쌓여야 신기록을 말한다`() {
        val m = { mm: Int, km: Int? -> expense(month = YearMonth.of(2026, mm), total = 1, drivenKm = km) }

        // 과거 2달뿐 → 침묵
        assertNull(personalRecordText(listOf(m(5, 100), m(6, 200), m(8, 900)), m(8, 900)))

        // 과거 3달 + 최고 기록 → 말한다
        val months = listOf(m(4, 300), m(5, 100), m(6, 200), m(8, 900))
        assertEquals(
            "기록을 시작한 뒤 가장 많이 달린 달이에요",
            personalRecordText(months, m(8, 900))
        )
    }

    @Test
    fun `거리 사다리 - 규모에 따라 비유가 바뀐다`() {
        assertTrue(distanceLadderText(40)!!.contains("여의도"))
        assertTrue(distanceLadderText(200)!!.contains("제주"))
        assertTrue(distanceLadderText(500)!!.contains("경부고속도로"))
        assertTrue(distanceLadderText(1_500)!!.contains("국토종주"))
        assertTrue(distanceLadderText(3_000)!!.contains("마라톤"))
        assertNull(distanceLadderText(0))
    }
}
