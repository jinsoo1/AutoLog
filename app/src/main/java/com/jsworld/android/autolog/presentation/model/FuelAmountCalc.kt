package com.jsworld.android.autolog.presentation.model

import kotlin.math.roundToInt

/** 금액 · 주유량 · 단가 세 값 중 어느 것을 사용자가 직접 넣었는지 구분하기 위한 식별자. */
enum class FuelField { AMOUNT, QUANTITY, UNIT_PRICE }

/**
 * 금액 · 주유량 · 단가는 서로 곱셈 관계다(금액 = 주유량 × 단가).
 * 사용자가 **둘만** 넣으면 남은 하나를 계산해준다.
 *
 * 세 값을 모두 직접 넣었다면 계산하지 않는다 — 사용자가 적은 값을 덮어쓰지 않기 위해서다.
 * (영수증 값이 반올림 때문에 딱 맞지 않는 경우가 흔하다)
 */
object FuelAmountCalc {

    /** 사용자가 직접 입력한 필드들로부터, 자동 계산될 필드를 정한다. 없으면 null. */
    fun autoField(edited: Set<FuelField>): FuelField? {
        if (edited.size != 2) return null
        return FuelField.entries.firstOrNull { it !in edited }
    }

    /**
     * @return 자동 계산된 값의 표시 문자열. 계산할 수 없으면 빈 문자열.
     */
    fun computeDisplay(
        target: FuelField,
        amount: Int?,
        quantity: Double?,
        unitPrice: Int?
    ): String = when (target) {
        FuelField.AMOUNT -> {
            val q = quantity
            val p = unitPrice
            if (q == null || p == null || q <= 0.0 || p <= 0) ""
            else (q * p).roundToInt().toString()
        }

        FuelField.QUANTITY -> {
            val a = amount
            val p = unitPrice
            if (a == null || p == null || a <= 0 || p <= 0) ""
            else formatQuantity(a.toDouble() / p)
        }

        FuelField.UNIT_PRICE -> {
            val a = amount
            val q = quantity
            if (a == null || q == null || a <= 0 || q <= 0.0) ""
            else (a / q).roundToInt().toString()
        }
    }

    /** 주유량은 소수 둘째 자리까지. 정수면 소수점을 붙이지 않는다. */
    fun formatQuantity(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString()
        else rounded.toString()
    }
}
