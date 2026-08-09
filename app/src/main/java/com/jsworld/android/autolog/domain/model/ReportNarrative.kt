package com.jsworld.android.autolog.domain.model

import kotlin.math.roundToInt

/**
 * 리포트 상단의 한 줄 내러티브 — 이 달이 어떤 달이었는지 표정을 만든다.
 * 숫자는 아래 카드들이 말하므로 여기서는 "분위기 + 어디를 보라"만 말한다.
 */
data class ReportNarrative(
    val title: String,
    val subtitle: String,
    val tone: NarrativeTone
)

enum class NarrativeTone { WARNING, SPIKE, PREP, EMPTY, CALM }

/** 지출 급증으로 볼 최소 증가액 / 배율 */
private const val SPIKE_MIN_WON = 100_000L

fun buildReportNarrative(
    current: MonthlyExpense,
    previous: MonthlyExpense?,
    /** 이번 달(최신 달)을 볼 때만 실제 값 — 과거 달에는 0을 넘긴다 */
    overdueCount: Int,
    soonCount: Int
): ReportNarrative = when {
    overdueCount > 0 -> ReportNarrative(
        title = "정비가 밀린 달이에요",
        subtitle = "교체 시기를 넘긴 항목이 ${overdueCount}개 있어요. 다가오는 지출에서 확인해보세요.",
        tone = NarrativeTone.WARNING
    )

    previous != null &&
        current.total - previous.total >= SPIKE_MIN_WON &&
        current.total >= previous.total + previous.total / 2 -> ReportNarrative(
        title = "큰 지출이 있었던 달이에요",
        subtitle = "지난달보다 %,d원 늘었어요. 원인은 총지출 카드에서 짚어드려요."
            .format(current.total - previous.total),
        tone = NarrativeTone.SPIKE
    )

    soonCount > 0 -> ReportNarrative(
        title = "교체 준비가 필요한 달이에요",
        subtitle = "곧 교체 시기가 오는 항목이 ${soonCount}개 있어요. 미리 준비하면 여유롭죠.",
        tone = NarrativeTone.PREP
    )

    current.total == 0L -> ReportNarrative(
        title = "기록이 없는 달이에요",
        subtitle = "이 달엔 지출 기록이 없어요.",
        tone = NarrativeTone.EMPTY
    )

    else -> ReportNarrative(
        title = "잔잔하게 지나간 달이에요",
        subtitle = "큰 일 없이 관리가 잘 이어지고 있어요.",
        tone = NarrativeTone.CALM
    )
}

/* ───────────────────────── 재미 지표 ───────────────────────── */

private const val EARTH_LAP_KM = 40_075.0

/** 차량 누적 주행거리 → "이 차와 함께 지구 1.8바퀴째 달리는 중이에요" */
fun earthLapsText(totalMileageKm: Int): String? {
    if (totalMileageKm <= 0) return null
    val laps = totalMileageKm / EARTH_LAP_KM
    return if (laps >= 1.0) {
        "이 차와 함께 지구 ${"%.1f".format(laps)}바퀴째 달리는 중이에요"
    } else {
        val percent = (laps * 100).roundToInt().coerceAtLeast(1)
        "이 차와 함께 지구 한 바퀴의 ${percent}%를 달렸어요"
    }
}

/**
 * 자기 기록 한 마디 — 비유가 아니라 내 데이터 안의 팩트라 유치하지 않다.
 * 비교할 과거가 3달 이상 쌓였을 때만 말한다(초기엔 아무 달이나 신기록이라서).
 */
fun personalRecordText(allMonths: List<MonthlyExpense>, current: MonthlyExpense): String? {
    val history = allMonths.filter { it.month < current.month }

    val kmHistory = history.mapNotNull { it.drivenKm }
    val currentKm = current.drivenKm
    if (currentKm != null && kmHistory.size >= 3) {
        if (currentKm > kmHistory.max()) return "기록을 시작한 뒤 가장 많이 달린 달이에요"
        val minPositiveKm = kmHistory.filter { it > 0 }.minOrNull()
        if (currentKm > 0 && minPositiveKm != null && currentKm < minPositiveKm) {
            return "기록을 시작한 뒤 가장 적게 달린 달이에요"
        }
    }

    val spentHistory = history.map { it.total }.filter { it > 0L }
    if (current.total > 0L && spentHistory.size >= 3 && current.total < spentHistory.min()) {
        return "기록을 시작한 뒤 가장 알뜰했던 달이에요"
    }
    return null
}

/**
 * 이 달 주행거리의 규모별 비유 — 자기 기록이 없는 조용한 달의 대체 문구.
 * 규모에 따라 비유가 바뀌어서 매달 다른 문장이 나온다.
 */
fun distanceLadderText(drivenKm: Int): String? = when {
    drivenKm <= 0 -> null
    drivenKm < 60 ->
        "이 달 주행 ${"%,d".format(drivenKm)}km — 여의도를 ${(drivenKm / 8.4).roundToInt().coerceAtLeast(1)}바퀴 돈 거리예요"
    drivenKm < 300 ->
        "이 달 주행 ${"%,d".format(drivenKm)}km — 제주 일주도로(181km) ${"%.1f".format(drivenKm / 181.0)}바퀴 거리예요"
    drivenKm < 800 ->
        "이 달 주행 ${"%,d".format(drivenKm)}km — 경부고속도로(416km)를 ${"%.1f".format(drivenKm / 416.0)}번 달린 거리예요"
    drivenKm < 2_500 ->
        "이 달 주행 ${"%,d".format(drivenKm)}km — 국토종주 자전거길(633km)을 ${"%.1f".format(drivenKm / 633.0)}번 완주한 거리예요"
    else ->
        "이 달 주행 ${"%,d".format(drivenKm)}km — 마라톤 풀코스 ${(drivenKm / 42.195).roundToInt()}번 거리예요"
}
