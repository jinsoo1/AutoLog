package com.jsworld.android.autolog

import com.jsworld.android.autolog.presentation.model.FuelAmountCalc
import com.jsworld.android.autolog.presentation.model.FuelField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 금액 · 주유량 · 단가 자동 계산.
 *
 * 화면에서 손으로 확인하기 어려운 반올림·0 처리를 여기서 고정한다.
 */
class FuelAmountCalcTest {

    // --- 어떤 필드가 자동 계산 대상인지 ---

    @Test
    fun `둘만 입력하면 남은 하나가 자동 계산 대상`() {
        assertEquals(
            FuelField.UNIT_PRICE,
            FuelAmountCalc.autoField(setOf(FuelField.AMOUNT, FuelField.QUANTITY))
        )
        assertEquals(
            FuelField.QUANTITY,
            FuelAmountCalc.autoField(setOf(FuelField.AMOUNT, FuelField.UNIT_PRICE))
        )
        assertEquals(
            FuelField.AMOUNT,
            FuelAmountCalc.autoField(setOf(FuelField.QUANTITY, FuelField.UNIT_PRICE))
        )
    }

    @Test
    fun `하나만 입력했으면 자동 계산하지 않는다`() {
        assertNull(FuelAmountCalc.autoField(emptySet()))
        assertNull(FuelAmountCalc.autoField(setOf(FuelField.AMOUNT)))
    }

    @Test
    fun `세 값을 모두 직접 입력했으면 덮어쓰지 않는다`() {
        // 영수증 값이 반올림 때문에 딱 맞지 않는 경우가 흔하므로 사용자 입력을 존중한다.
        assertNull(FuelAmountCalc.autoField(FuelField.entries.toSet()))
    }

    // --- 실제 계산 ---

    @Test
    fun `금액과 주유량으로 단가를 구한다`() {
        assertEquals(
            "1680",
            FuelAmountCalc.computeDisplay(
                FuelField.UNIT_PRICE, amount = 71400, quantity = 42.5, unitPrice = null
            )
        )
    }

    @Test
    fun `금액과 단가로 주유량을 구한다`() {
        assertEquals(
            "42.5",
            FuelAmountCalc.computeDisplay(
                FuelField.QUANTITY, amount = 71400, quantity = null, unitPrice = 1680
            )
        )
    }

    @Test
    fun `주유량과 단가로 금액을 구한다`() {
        assertEquals(
            "71400",
            FuelAmountCalc.computeDisplay(
                FuelField.AMOUNT, amount = null, quantity = 42.5, unitPrice = 1680
            )
        )
    }

    @Test
    fun `단가는 반올림한다`() {
        // 50000 / 30.0 = 1666.66... → 1667
        assertEquals(
            "1667",
            FuelAmountCalc.computeDisplay(
                FuelField.UNIT_PRICE, amount = 50000, quantity = 30.0, unitPrice = null
            )
        )
    }

    @Test
    fun `0이나 빈 값이면 계산하지 않는다`() {
        assertEquals(
            "",
            FuelAmountCalc.computeDisplay(
                FuelField.UNIT_PRICE, amount = 50000, quantity = 0.0, unitPrice = null
            )
        )
        assertEquals(
            "",
            FuelAmountCalc.computeDisplay(
                FuelField.QUANTITY, amount = null, quantity = null, unitPrice = 1680
            )
        )
        assertEquals(
            "",
            FuelAmountCalc.computeDisplay(
                FuelField.AMOUNT, amount = null, quantity = 42.5, unitPrice = 0
            )
        )
    }

    // --- 주유량 표기 ---

    @Test
    fun `주유량은 소수 둘째 자리까지 정수면 소수점을 붙이지 않는다`() {
        assertEquals("30", FuelAmountCalc.formatQuantity(30.0))
        assertEquals("42.5", FuelAmountCalc.formatQuantity(42.5))
        assertEquals("29.76", FuelAmountCalc.formatQuantity(29.7619))
    }
}
