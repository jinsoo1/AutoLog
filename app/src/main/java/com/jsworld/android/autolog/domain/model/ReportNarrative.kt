package com.jsworld.android.autolog.domain.model

import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.ui.UiText

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
