package com.jsworld.android.autolog.domain.model

import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.UiText
import kotlin.math.roundToInt

/**
 * 리포트 상단의 한 줄 내러티브 — 이 달이 어떤 달이었는지 표정을 만든다.
 * 숫자는 아래 카드들이 말하므로 여기서는 "분위기 + 어디를 보라"만 말한다.
 */
data class ReportNarrative(
    val title: UiText,
    val subtitle: UiText,
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
        title = UiText.Res(R.string.narrative_overdue_title),
        subtitle = UiText.Res(R.string.narrative_overdue_subtitle, overdueCount),
        tone = NarrativeTone.WARNING
    )

    previous != null &&
        current.total - previous.total >= SPIKE_MIN_WON &&
        current.total >= previous.total + previous.total / 2 -> ReportNarrative(
        title = UiText.Res(R.string.narrative_spike_title),
        subtitle = UiText.Res(R.string.narrative_spike_subtitle, current.total - previous.total),
        tone = NarrativeTone.SPIKE
    )

    soonCount > 0 -> ReportNarrative(
        title = UiText.Res(R.string.narrative_prep_title),
        subtitle = UiText.Res(R.string.narrative_prep_subtitle, soonCount),
        tone = NarrativeTone.PREP
    )

    current.total == 0L -> ReportNarrative(
        title = UiText.Res(R.string.narrative_empty_title),
        subtitle = UiText.Res(R.string.narrative_empty_subtitle),
        tone = NarrativeTone.EMPTY
    )

    else -> ReportNarrative(
        title = UiText.Res(R.string.narrative_calm_title),
        subtitle = UiText.Res(R.string.narrative_calm_subtitle),
        tone = NarrativeTone.CALM
    )
}

/* ───────────────────────── 재미 지표 ───────────────────────── */

private const val EARTH_LAP_KM = 40_075.0

/** 차량 누적 주행거리 → "이 차와 함께 지구 1.8바퀴째 달리는 중이에요" */
fun earthLapsText(totalMileageKm: Int): UiText? {
    if (totalMileageKm <= 0) return null
    val laps = totalMileageKm / EARTH_LAP_KM
    return if (laps >= 1.0) {
        UiText.Res(R.string.fun_earth_laps, laps)
    } else {
        val percent = (laps * 100).roundToInt().coerceAtLeast(1)
        UiText.Res(R.string.fun_earth_percent, percent)
    }
}

/**
 * 자기 기록 한 마디 — 비유가 아니라 내 데이터 안의 팩트라 유치하지 않다.
 * 비교할 과거가 3달 이상 쌓였을 때만 말한다(초기엔 아무 달이나 신기록이라서).
 */
fun personalRecordText(allMonths: List<MonthlyExpense>, current: MonthlyExpense): UiText? {
    val history = allMonths.filter { it.month < current.month }

    val kmHistory = history.mapNotNull { it.drivenKm }
    val currentKm = current.drivenKm
    if (currentKm != null && kmHistory.size >= 3) {
        if (currentKm > kmHistory.max()) return UiText.Res(R.string.fun_record_most_driven)
        val minPositiveKm = kmHistory.filter { it > 0 }.minOrNull()
        if (currentKm > 0 && minPositiveKm != null && currentKm < minPositiveKm) {
            return UiText.Res(R.string.fun_record_least_driven)
        }
    }

    val spentHistory = history.map { it.total }.filter { it > 0L }
    if (current.total > 0L && spentHistory.size >= 3 && current.total < spentHistory.min()) {
        return UiText.Res(R.string.fun_record_thriftiest)
    }
    return null
}

/**
 * 거리 비유의 기준 거리. 나라마다 "누구나 아는 거리"가 다르므로 로케일별로 고른다 —
 * 리소스 문구(fun_ladder_tier*)에 적힌 이름·거리와 반드시 짝이 맞아야 한다.
 */
data class DistanceLandmarks(
    val tier1Km: Double,
    val tier2Km: Double,
    val tier3Km: Double,
    val tier4Km: Double
) {
    companion object {
        /** 여의도 둘레 · 제주 일주도로 · 경부고속도로 · 국토종주 자전거길 */
        val KOREA = DistanceLandmarks(8.4, 181.0, 416.0, 633.0)

        /** 야마노테선 한 바퀴 · 하코네 에키덴 왕복 · 도쿄~신오사카 · 도쿄~하카타 */
        val JAPAN = DistanceLandmarks(34.5, 217.1, 552.6, 1174.9)

        fun forLanguage(language: String): DistanceLandmarks =
            if (language == "ja") JAPAN else KOREA
    }
}

private const val MARATHON_KM = 42.195

/**
 * 이 달 주행거리의 규모별 비유 — 자기 기록이 없는 조용한 달의 대체 문구.
 * 규모에 따라 비유가 바뀌어서 매달 다른 문장이 나온다.
 */
fun distanceLadderText(
    drivenKm: Int,
    landmarks: DistanceLandmarks = DistanceLandmarks.KOREA
): UiText? = when {
    drivenKm <= 0 -> null
    drivenKm < 60 ->
        UiText.Res(
            R.string.fun_ladder_tier1,
            drivenKm,
            (drivenKm / landmarks.tier1Km).roundToInt().coerceAtLeast(1)
        )
    drivenKm < 300 ->
        UiText.Res(R.string.fun_ladder_tier2, drivenKm, drivenKm / landmarks.tier2Km)
    drivenKm < 800 ->
        UiText.Res(R.string.fun_ladder_tier3, drivenKm, drivenKm / landmarks.tier3Km)
    drivenKm < 2_500 ->
        UiText.Res(R.string.fun_ladder_tier4, drivenKm, drivenKm / landmarks.tier4Km)
    else ->
        UiText.Res(R.string.fun_ladder_tier5, drivenKm, (drivenKm / MARATHON_KM).roundToInt())
}
