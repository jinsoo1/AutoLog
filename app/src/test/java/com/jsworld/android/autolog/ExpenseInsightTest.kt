package com.jsworld.android.autolog

import com.jsworld.android.autolog.domain.model.MonthlyExpense
import com.jsworld.android.autolog.domain.model.buildExpenseInsight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ExpenseInsightTest {

    private fun month(fuel: Long, maint: Long = 0, care: Long = 0) = MonthlyExpense(
        month = YearMonth.of(2026, 8),
        fuelCost = fuel,
        maintenanceCost = maint,
        careCost = care,
        missingCostCount = 0,
        drivenKm = null
    )

    @Test
    fun `증가하면 더 썼다고, 감소하면 덜 썼다고 말한다`() {
        val up = buildExpenseInsight(month(200_000), month(150_000))
        assertTrue(up.headline.contains("50,000원 더 썼어요"))
        assertTrue(up.direction > 0)

        val down = buildExpenseInsight(month(100_000), month(150_000))
        assertTrue(down.headline.contains("50,000원 덜 썼어요"))
        assertTrue(down.direction < 0)
    }

    @Test
    fun `정비 증가가 원인이고 최대 항목이 절반 이상이면 항목명으로 짚는다`() {
        val insight = buildExpenseInsight(
            current = month(fuel = 150_000, maint = 480_000),
            previous = month(fuel = 150_000, maint = 30_000),
            topMaintenanceName = "타이어 교체",
            topMaintenanceCost = 450_000
        )
        assertEquals("늘어난 금액의 대부분은 타이어 교체 450,000원이에요", insight.detail)
    }

    @Test
    fun `반대로 움직인 카테고리가 있으면 한 마디 덧붙인다`() {
        val insight = buildExpenseInsight(
            current = month(fuel = 130_000, maint = 480_000),
            previous = month(fuel = 151_000, maint = 30_000),
            topMaintenanceName = "타이어 교체",
            topMaintenanceCost = 450_000
        )
        assertTrue(insight.detail!!.contains("주유·충전은 오히려 21,000원 줄었어요"))
    }

    @Test
    fun `주유가 원인이면 카테고리로 말한다`() {
        val insight = buildExpenseInsight(month(250_000), month(150_000))
        assertEquals("주유·충전 지출이 100,000원 늘었어요", insight.detail)
    }

    @Test
    fun `잔변동이면 원인을 지어내지 않는다`() {
        val insight = buildExpenseInsight(month(153_000), month(150_000))
        assertNull(insight.detail)
    }
}
