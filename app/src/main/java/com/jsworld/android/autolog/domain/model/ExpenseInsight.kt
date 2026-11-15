package com.jsworld.android.autolog.domain.model

import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.LocalizedNames
import com.jsworld.android.autolog.core.ui.UiText
import kotlin.math.abs

/**
 * 총지출 카드의 "왜 이만큼 썼는지" 요약.
 * 증감 숫자만 주지 않고, 어느 카테고리(가능하면 어느 항목) 때문인지 짚는다.
 */
data class ExpenseInsight(
    /** "지난달보다 182,000원 더 썼어요" */
    val headline: UiText,
    /** "늘어난 금액의 대부분은 타이어 교체 450,000원이에요 · 주유는 오히려 21,000원 줄었어요" */
    val detail: UiText?,
    /** 양수 = 증가(경고 톤), 음수 = 감소(긍정 톤), 0 = 비슷 */
    val direction: Int
)

/** 항목별 마지막 교체 비용 — 다가오는 지출 예상에 쓴다 */
data class SettingLastCost(
    val settingId: Long,
    val cost: Int?
)

/** detail 문장을 만들 가치가 없는 잔변동 기준 */
private const val NOISE_WON = 10_000L

fun buildExpenseInsight(
    current: MonthlyExpense,
    previous: MonthlyExpense,
    /** 이번 달 가장 비싼 정비·수리 기록 — 증가 원인을 항목명으로 짚을 때 쓴다 */
    topMaintenanceName: String? = null,
    topMaintenanceCost: Int? = null
): ExpenseInsight {
    val diff = current.total - previous.total

    val headline = when {
        diff > 0L -> UiText.Res(R.string.insight_more, diff)
        diff < 0L -> UiText.Res(R.string.insight_less, -diff)
        else -> UiText.Res(R.string.insight_same)
    }

    val deltas = listOf(
        R.string.category_fuel to (current.fuelCost - previous.fuelCost),
        R.string.category_maintenance to (current.maintenanceCost - previous.maintenanceCost),
        R.string.category_care to (current.careCost - previous.careCost)
    )
    val dominant = deltas.maxByOrNull { abs(it.second) }
        ?.takeIf { abs(it.second) >= NOISE_WON }

    val detail = dominant?.let { (label, delta) ->
        val main: UiText =
            if (label == R.string.category_maintenance && delta > 0L &&
                topMaintenanceName != null && topMaintenanceCost != null &&
                topMaintenanceCost >= delta / 2
            ) {
                UiText.Res(
                    R.string.insight_dominant_item,
                    LocalizedNames.item(topMaintenanceName),
                    topMaintenanceCost.toLong()
                )
            } else {
                UiText.Res(
                    if (delta > 0L) R.string.insight_category_up else R.string.insight_category_down,
                    UiText.Res(label),
                    abs(delta)
                )
            }

        // 반대 방향으로 움직인 카테고리가 있으면 한 마디 덧붙인다.
        val counter: UiText? = deltas
            .filter { it !== dominant && abs(it.second) >= NOISE_WON }
            .filter { it.second * delta < 0L }
            .maxByOrNull { abs(it.second) }
            ?.let { (cLabel, cDelta) ->
                UiText.Res(
                    if (cDelta > 0L) R.string.insight_counter_up else R.string.insight_counter_down,
                    UiText.Res(cLabel),
                    abs(cDelta)
                )
            }

        if (counter == null) main else UiText.Joined(listOf(main, counter), separator = "")
    }

    return ExpenseInsight(
        headline = headline,
        detail = detail,
        direction = diff.compareTo(0L)
    )
}

