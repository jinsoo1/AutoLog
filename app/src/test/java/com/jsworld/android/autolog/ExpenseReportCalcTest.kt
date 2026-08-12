package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.ExpenseCostRow
import com.jsworld.android.autolog.domain.model.ExpenseReportCalc
import com.jsworld.android.autolog.domain.model.MileagePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ExpenseReportCalcTest {

    private val may = YearMonth.of(2026, 5)

    @Test
    fun `기록이 없으면 빈 목록`() {
        assertTrue(
            ExpenseReportCalc.build(emptyMap(), emptyList(), emptyList(), may).isEmpty()
        )
    }

    @Test
    fun `기록 없는 달도 0원으로 채워 이어진다`() {
        val result = ExpenseReportCalc.build(
            fuelByMonth = mapOf("2026-03" to 50_000L),
            maintenanceRows = listOf(ExpenseCostRow("2026-05", "엔진오일", 80_000)),
            mileagePoints = emptyList(),
            current = may
        )
        assertEquals(listOf(3, 4, 5), result.map { it.month.monthValue })
        assertEquals(0L, result[1].total) // 4월은 기록 없음
        assertEquals(50_000L, result[0].fuelCost)
        assertEquals(80_000L, result[2].maintenanceCost)
    }

    @Test
    fun `isCare 항목은 관리 비용으로, 나머지는 정비 비용으로 분류된다`() {
        val result = ExpenseReportCalc.build(
            fuelByMonth = emptyMap(),
            maintenanceRows = listOf(
                // 분류는 이름이 아니라 타입 플래그(isCare)로 한다 — '실내 클리닝'처럼
                // 키워드가 없는 관리 항목도 세차로 집계되어야 하기 때문.
                ExpenseCostRow("2026-05", "세차", 20_000, isCare = true),
                ExpenseCostRow("2026-05", "엔진오일", 80_000),
                ExpenseCostRow("2026-05", "써모스탯 교체", 150_000)
            ),
            mileagePoints = emptyList(),
            current = may
        )
        val m = result.single()
        assertEquals(20_000L, m.careCost)
        assertEquals(230_000L, m.maintenanceCost)
        assertEquals(250_000L, m.total)
    }

    @Test
    fun `금액 미입력 기록은 합계에서 빠지고 개수로 보고된다`() {
        val m = ExpenseReportCalc.build(
            fuelByMonth = emptyMap(),
            maintenanceRows = listOf(
                ExpenseCostRow("2026-05", "엔진오일", 80_000),
                ExpenseCostRow("2026-05", "타이어 교체", null)
            ),
            mileagePoints = emptyList(),
            current = may
        ).single()
        assertEquals(80_000L, m.total)
        assertEquals(1, m.missingCostCount)
    }

    @Test
    fun `월 주행거리 - 달 시작 전 기준점이 없으면 계산 불가`() {
        val points = listOf(MileagePoint("2026-05-10", 50_000))
        assertNull(ExpenseReportCalc.drivenKmIn(may, points.sortedBy { it.date }))
    }

    @Test
    fun `월 주행거리 - 이전 최대 누적과 달 끝 최대 누적의 차`() {
        val points = listOf(
            MileagePoint("2026-04-20", 48_000),
            MileagePoint("2026-05-05", 49_000),
            MileagePoint("2026-05-25", 50_300)
        ).sortedBy { it.date }
        assertEquals(2_300, ExpenseReportCalc.drivenKmIn(may, points))
    }

    @Test
    fun `월 주행거리 - 과거 날짜에 낮은 값이 늦게 들어와도 음수가 되지 않는다`() {
        val points = listOf(
            MileagePoint("2026-04-20", 48_000),
            MileagePoint("2026-05-05", 47_000) // 과거 기록을 뒤늦게 입력한 경우
        ).sortedBy { it.date }
        assertEquals(0, ExpenseReportCalc.drivenKmIn(may, points))
    }

    @Test
    fun `km당 유지비 - 주행거리를 모르거나 0이면 null`() {
        val withKm = ExpenseReportCalc.build(
            fuelByMonth = mapOf("2026-05" to 100_000L),
            maintenanceRows = emptyList(),
            mileagePoints = listOf(
                MileagePoint("2026-04-01", 10_000),
                MileagePoint("2026-05-20", 10_500)
            ),
            current = may
        ).single()
        assertEquals(200, withKm.costPerKm) // 100,000원 / 500km

        val noKm = ExpenseReportCalc.build(
            fuelByMonth = mapOf("2026-05" to 100_000L),
            maintenanceRows = emptyList(),
            mileagePoints = emptyList(),
            current = may
        ).single()
        assertNull(noKm.costPerKm)
    }
}
