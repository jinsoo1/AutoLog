package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.CarMaintenanceRecord
import com.jsworld.android.autolog.domain.model.FuelRecord
import com.jsworld.android.autolog.domain.model.FuelUnit
import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.NarrativeTone
import com.jsworld.android.autolog.domain.model.buildYearHighlights
import com.jsworld.android.autolog.domain.model.buildYearNarrative
import com.jsworld.android.autolog.domain.model.topSpendItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ReportYearlyTest {

    private fun maint(name: String, cost: Int?, date: String = "2026-03-10", repair: Boolean = false) =
        CarMaintenanceRecord(
            historyId = 0, settingId = 0, typeId = 0, typeName = name,
            serviceDate = date, serviceMileage = null, place = null,
            cost = cost, memo = null, isRepair = repair
        )

    private fun fuel(
        amount: Int?, qty: Double?, date: String = "2026-05-01",
        station: String? = null, unit: FuelUnit = FuelUnit.LITER
    ) = FuelRecord(
        id = 0, carId = 1, filledAt = date, mileage = null,
        amount = amount, quantity = qty, unitPrice = null,
        unit = unit, station = station, memo = null, photoPath = null
    )

    private fun month(mm: Int, total: Long = 0, drivenKm: Int? = null) = MonthlyExpense(
        month = YearMonth.of(2026, mm),
        fuelCost = total, maintenanceCost = 0, careCost = 0,
        missingCostCount = 0, drivenKm = drivenKm
    )

    @Test
    fun `연간 내러티브 - 수리 2건이면 수리가 잦았던 해`() {
        val n = buildYearNarrative(500_000, null, repairCount = 2, isCompleteYear = false)
        assertTrue(n.title.contains("수리가 잦았던"))
    }

    @Test
    fun `연간 내러티브 - 진행 중인 해에는 알뜰했다고 단정하지 않는다`() {
        val ongoing = buildYearNarrative(100_000, 1_000_000, 0, isCompleteYear = false)
        assertEquals(NarrativeTone.CALM, ongoing.tone)
        assertTrue(!ongoing.title.contains("알뜰"))

        val complete = buildYearNarrative(100_000, 1_000_000, 0, isCompleteYear = true)
        assertTrue(complete.title.contains("알뜰"))
    }

    @Test
    fun `항목별 TOP - 같은 항목은 합산되고 금액순 정렬`() {
        val items = topSpendItems(
            listOf(
                maint("엔진오일", 90_000),
                maint("타이어 교체", 450_000),
                maint("엔진오일", 90_000),
                maint("세차", 20_000),
                maint("금액없음", null)
            )
        )
        assertEquals(listOf("타이어 교체", "엔진오일", "세차"), items.map { it.name })
        assertEquals(180_000L, items[1].total)
        assertEquals(2, items[1].count)
        assertTrue(items[2].isCare)
    }

    @Test
    fun `올해의 기록 - 단골은 2회 이상일 때만`() {
        val once = buildYearHighlights(
            emptyList(),
            listOf(fuel(50_000, 30.0, station = "GS칼텍스")),
            emptyList()
        )
        assertNull(once.find { it.label == "단골" })

        val twice = buildYearHighlights(
            emptyList(),
            listOf(
                fuel(50_000, 30.0, station = "GS칼텍스"),
                fuel(50_000, 30.0, station = "GS칼텍스"),
                fuel(50_000, 30.0, station = "S-OIL")
            ),
            emptyList()
        )
        assertEquals("GS칼텍스 · 2회", twice.find { it.label == "단골" }?.value)
    }

    @Test
    fun `올해의 기록 - 주유량은 드럼통으로 환산된다`() {
        val h = buildYearHighlights(
            emptyList(),
            listOf(fuel(1, 1_240.0)),
            emptyList()
        )
        assertEquals("1,240L · 드럼통 6.2개 분량", h.find { it.label == "올해 주유량" }?.value)
    }

    @Test
    fun `올해의 기록 - 작년 단가 비교는 2년치가 있어야 나온다`() {
        val noPrev = buildYearHighlights(
            emptyList(), listOf(fuel(48_000, 30.0)), emptyList(), prevYearFuel = emptyList()
        )
        assertNull(noPrev.find { it.label == "주유 단가" })

        val withPrev = buildYearHighlights(
            emptyList(),
            listOf(fuel(48_000, 30.0)),            // 1,600원/L
            emptyList(),
            prevYearFuel = listOf(fuel(49_350, 30.0, date = "2025-05-01")) // 1,645원/L
        )
        assertEquals(
            "작년보다 L당 45원 저렴",
            withPrev.find { it.label == "주유 단가" }?.value
        )
    }

    @Test
    fun `올해의 기록 - 알뜰한 달은 지출 있는 달이 둘 이상일 때만`() {
        val one = buildYearHighlights(listOf(month(3, 50_000)), emptyList(), emptyList())
        assertNull(one.find { it.label == "가장 알뜰했던 달" })

        val two = buildYearHighlights(
            listOf(month(3, 50_000), month(4, 30_000)), emptyList(), emptyList()
        )
        assertEquals("4월 · 30,000원", two.find { it.label == "가장 알뜰했던 달" }?.value)
    }
}
