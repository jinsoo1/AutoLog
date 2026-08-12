package com.jsworld.android.autolog.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 세차·관리 허브 통계 — 정비(마감 중심)와 달리 "마지막으로 한 지 얼마나 됐나"가 축이다.
 * 전부 순수 함수라 단위 테스트로 지킨다.
 */

/** 이름에 '세차'가 들어가면 세차 기록 — 경과일 히어로의 기준 */
fun isWashName(name: String): Boolean = name.contains("세차")

data class CareOverview(
    /** 마지막 세차 후 경과일. 세차 기록이 없으면 null */
    val daysSinceWash: Int?,
    /** 마지막 세차 기록 (날짜·비용·메모 표시용) */
    val lastWash: CareRecord?,
    /** 내 데이터 기반 평균 세차 간격(일). 세차 3회 미만이면 null */
    val averageIntervalDays: Int?,
    /** 평균 간격을 넘겼는지 — "슬슬 때가 됐네요" 넛지 */
    val isDue: Boolean
)

fun buildCareOverview(
    careRecords: List<CareRecord>,
    today: LocalDate
): CareOverview {
    val washes = careRecords
        .filter { isWashName(it.itemName) && it.performedAt != null }
        .sortedBy { it.performedAt }

    val last = washes.lastOrNull()
    val lastDate = last?.performedAt?.toLocalDateOrNull()
    val daysSince = lastDate?.let { ChronoUnit.DAYS.between(it, today).toInt() }

    // 평균 간격 = (마지막 - 첫) / (기록 수 - 1). 같은 날 여러 번은 하루로 본다.
    val dates = washes.mapNotNull { it.performedAt?.toLocalDateOrNull() }.distinct()
    val avg = if (dates.size >= 3) {
        val span = ChronoUnit.DAYS.between(dates.first(), dates.last()).toInt()
        (span / (dates.size - 1)).coerceAtLeast(1)
    } else null

    return CareOverview(
        daysSinceWash = daysSince,
        lastWash = last,
        averageIntervalDays = avg,
        isDue = daysSince != null && avg != null && daysSince >= avg
    )
}

data class CareCounts(
    val monthCount: Int,
    val yearCount: Int,
    val yearCost: Long
)

fun careCounts(careRecords: List<CareRecord>, today: LocalDate): CareCounts {
    val monthPrefix = "%04d-%02d".format(today.year, today.monthValue)
    val yearPrefix = "%04d".format(today.year)
    val thisYear = careRecords.filter { it.performedAt?.startsWith(yearPrefix) == true }
    return CareCounts(
        monthCount = careRecords.count { it.performedAt?.startsWith(monthPrefix) == true },
        yearCount = thisYear.size,
        yearCost = thisYear.sumOf { (it.cost ?: 0).toLong() }
    )
}

/** 코팅·왁스 같은 세차 외 관리 항목의 마지막 시점 — "코팅 45일 전" 줄 */
fun upkeepLines(
    careRecords: List<CareRecord>,
    today: LocalDate
): List<Pair<String, Int>> =
    careRecords
        .filter { !isWashName(it.itemName) && it.performedAt != null }
        .groupBy { it.itemName }
        .mapNotNull { (name, records) ->
            records.mapNotNull { it.performedAt?.toLocalDateOrNull() }.maxOrNull()
                ?.let { name to ChronoUnit.DAYS.between(it, today).toInt() }
        }
        .sortedBy { it.second }

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()
