package com.jsworld.android.autolog.domain.model

import kotlin.math.abs

/**
 * 총지출 카드의 "왜 이만큼 썼는지" 요약.
 * 증감 숫자만 주지 않고, 어느 카테고리(가능하면 어느 항목) 때문인지 짚는다.
 */
data class ExpenseInsight(
    /** "지난달보다 182,000원 더 썼어요" */
    val headline: String,
    /** "늘어난 금액의 대부분은 타이어 교체 450,000원이에요 · 주유는 오히려 21,000원 줄었어요" */
    val detail: String?,
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
        diff > 0L -> "지난달보다 ${diff.won()}원 더 썼어요"
        diff < 0L -> "지난달보다 ${(-diff).won()}원 덜 썼어요"
        else -> "지난달과 지출이 같아요"
    }

    val deltas = listOf(
        "주유·충전" to (current.fuelCost - previous.fuelCost),
        "정비·수리" to (current.maintenanceCost - previous.maintenanceCost),
        "세차·관리" to (current.careCost - previous.careCost)
    )
    val dominant = deltas.maxByOrNull { abs(it.second) }
        ?.takeIf { abs(it.second) >= NOISE_WON }

    val detail = dominant?.let { (label, delta) ->
        val main =
            if (label == "정비·수리" && delta > 0L &&
                topMaintenanceName != null && topMaintenanceCost != null &&
                topMaintenanceCost >= delta / 2
            ) {
                "늘어난 금액의 대부분은 $topMaintenanceName ${topMaintenanceCost.toLong().won()}원이에요"
            } else {
                "$label 지출이 ${abs(delta).won()}원 ${if (delta > 0L) "늘었어요" else "줄었어요"}"
            }

        // 반대 방향으로 움직인 카테고리가 있으면 한 마디 덧붙인다.
        val counter = deltas
            .filter { it !== dominant && abs(it.second) >= NOISE_WON }
            .filter { it.second * delta < 0L }
            .maxByOrNull { abs(it.second) }
            ?.let { (cLabel, cDelta) ->
                " · ${cLabel}은 오히려 ${abs(cDelta).won()}원 ${if (cDelta > 0L) "늘었어요" else "줄었어요"}"
            }
            .orEmpty()

        main + counter
    }

    return ExpenseInsight(
        headline = headline,
        detail = detail,
        direction = diff.compareTo(0L)
    )
}

private fun Long.won(): String = "%,d".format(this)
