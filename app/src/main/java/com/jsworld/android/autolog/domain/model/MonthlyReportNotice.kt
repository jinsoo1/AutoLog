package com.jsworld.android.autolog.domain.model

import java.time.YearMonth
import java.time.ZonedDateTime

/**
 * 월간 리포트 도착 알림 — 매월 1일 "지난달 총 N원"을 알려 리포트로 부른다.
 * 리포트는 만들어두고 아무도 안 여는 게 최대 리스크라, 도착을 알리는 게 핵심이다.
 * 알림 여부·내용 계산은 전부 순수 함수로 두고 테스트로 지킨다.
 */

data class MonthlyReportCarLine(val carName: String, val total: Long)

data class MonthlyReportNotice(
    val month: YearMonth,
    /** 전 차량 합계(금액 입력된 기록만) */
    val total: Long,
    /** 금액 미입력으로 합계에서 빠진 기록 수 */
    val missingCostCount: Int,
    /** 차량이 2대 이상일 때만 — 알림 본문의 차량별 줄 */
    val lines: List<MonthlyReportCarLine>
)

/**
 * 지난달 리포트 알림 내용. **알릴 게 없으면 null** — 기록이 없는 달에
 * "0원 리포트" 알림을 보내는 건 정보가 아니라 소음이다.
 */
fun buildMonthlyReportNotice(
    month: YearMonth,
    carExpenses: List<Pair<String, List<MonthlyExpense>>>
): MonthlyReportNotice? {
    val present = carExpenses.mapNotNull { (name, expenses) ->
        expenses.firstOrNull { it.month == month }?.let { name to it }
    }
    if (present.isEmpty()) return null

    val total = present.sumOf { it.second.total }
    val missing = present.sumOf { it.second.missingCostCount }
    // 합계도 0, 빠진 기록도 0이면 그 달엔 아무 일도 없었다.
    if (total == 0L && missing == 0) return null

    return MonthlyReportNotice(
        month = month,
        total = total,
        missingCostCount = missing,
        lines = if (present.size >= 2) {
            present.map { MonthlyReportCarLine(it.first, it.second.total) }
        } else {
            emptyList()
        }
    )
}

/** 다음 실행 시각 = 돌아오는 1일 [hour]시. 이미 지났으면 다음 달 1일 */
fun nextMonthlyReportTime(now: ZonedDateTime, hour: Int): ZonedDateTime {
    val thisMonthFirst = now.withDayOfMonth(1)
        .withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
    return if (thisMonthFirst.isAfter(now)) thisMonthFirst else thisMonthFirst.plusMonths(1)
}
