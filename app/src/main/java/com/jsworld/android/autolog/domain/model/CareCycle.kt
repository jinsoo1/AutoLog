package com.jsworld.android.autolog.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 세차·관리 항목의 주기와 진행도.
 *
 * 정비와 결정적으로 다른 점: **세차 횟수**를 단위로 쓸 수 있다.
 * "세차 3번 중 1번은 실내 클리닝"처럼 km·개월로는 표현할 수 없는 리듬을
 * 세차 기록 수를 세어 계산한다.
 */

/** 세차 허브의 항목 관리 목록 한 줄 */
data class CarePickItem(
    val name: String,
    val enabled: Boolean,
    /** 켜져 있을 때만 존재 */
    val settingId: Long?,
    val intervalMonths: Int?,
    val intervalWashCount: Int?
)

enum class CareCycleUnit { WASH_COUNT, MONTHS, NONE }

/** 세차 허브의 '관리 주기' 한 줄 */
data class CareCycleProgress(
    val settingId: Long,
    val name: String,
    val unit: CareCycleUnit,
    /** 0~1. 주기가 없으면 null */
    val progress: Float?,
    /** "1회 남음" / "2회 지남" / "135일 남음" — 사람이 읽는 문장 */
    val remainText: String,
    /** 주기를 넘겼는지. 빨간 경고가 아니라 차분한 강조에만 쓴다 */
    val isOverdue: Boolean,
    /** "세차 3회마다 · 마지막 7월 2일" 같은 보조 설명 */
    val caption: String
)

/** 주기 선택지 — 세차 횟수 / 개월 */
val CARE_WASH_COUNT_OPTIONS = listOf(2, 3, 5, 10)
val CARE_MONTH_OPTIONS = listOf(1, 3, 6, 12)

/**
 * 세차 허브의 관리 주기 목록을 만든다.
 *
 * @param washDates 세차(카운터 기준) 기록의 날짜들 — 오름차순일 필요는 없다.
 * @param lastByName 항목 이름별 마지막 기록 날짜.
 */
fun buildCareCycles(
    items: List<CarePickItem>,
    washDates: List<String>,
    lastByName: Map<String, String>,
    today: LocalDate
): List<CareCycleProgress> {
    val sortedWashes = washDates.distinct().sorted()

    return items.mapNotNull { item ->
        val settingId = item.settingId ?: return@mapNotNull null
        // 카운터 기준인 '세차' 자체에는 주기를 매기지 않는다(경과일 히어로가 그 역할).
        if (isWashName(item.name) && item.intervalWashCount == null && item.intervalMonths == null) {
            return@mapNotNull null
        }

        val last = lastByName[item.name]

        when {
            item.intervalWashCount != null && item.intervalWashCount > 0 -> {
                val n = item.intervalWashCount
                // 마지막 기록 이후의 세차 횟수. 기록이 없으면 전체 세차 횟수를 쓴다.
                val since = if (last == null) sortedWashes.size
                else sortedWashes.count { it > last }
                val remaining = n - since

                CareCycleProgress(
                    settingId = settingId,
                    name = item.name,
                    unit = CareCycleUnit.WASH_COUNT,
                    progress = (since.toFloat() / n).coerceIn(0f, 1f),
                    remainText = when {
                        remaining > 0 -> "${remaining}회 남음"
                        remaining == 0 -> "이번에 할 때"
                        else -> "${-remaining}회 지남"
                    },
                    isOverdue = remaining <= 0,
                    caption = buildString {
                        append("세차 ${n}회마다")
                        append(" · ")
                        append(
                            if (last == null) "아직 기록 없음"
                            else "마지막 ${last.toShortDateText()}"
                        )
                    }
                )
            }

            item.intervalMonths != null && item.intervalMonths > 0 -> {
                val lastDate = last?.toLocalDateOrNull()
                if (lastDate == null) {
                    CareCycleProgress(
                        settingId = settingId,
                        name = item.name,
                        unit = CareCycleUnit.MONTHS,
                        progress = null,
                        remainText = "첫 기록 필요",
                        isOverdue = false,
                        caption = "${item.intervalMonths}개월마다 · 아직 기록 없음"
                    )
                } else {
                    val due = lastDate.plusMonths(item.intervalMonths.toLong())
                    val remainingDays = ChronoUnit.DAYS.between(today, due)
                    val total = ChronoUnit.DAYS.between(lastDate, due).coerceAtLeast(1)
                    val used = ChronoUnit.DAYS.between(lastDate, today).coerceAtLeast(0)

                    CareCycleProgress(
                        settingId = settingId,
                        name = item.name,
                        unit = CareCycleUnit.MONTHS,
                        progress = (used.toFloat() / total).coerceIn(0f, 1f),
                        remainText = when {
                            remainingDays > 0 -> "${remainingDays}일 남음"
                            remainingDays == 0L -> "오늘까지"
                            else -> "${-remainingDays}일 지남"
                        },
                        isOverdue = remainingDays < 0,
                        caption = "${item.intervalMonths}개월마다 · 마지막 ${lastDate.toShortDateText()}"
                    )
                }
            }

            else -> null // 주기 없음 — 기록만 남기는 항목
        }
    }.sortedWith(compareByDescending<CareCycleProgress> { it.isOverdue }.thenBy { it.name })
}

/**
 * 세차를 저장한 직후 "이번엔 왁스도 할 때예요" 안내를 띄울 항목.
 * 세차 횟수 주기가 도달·초과한 것만 — 기간 주기는 세차와 무관하므로 제외한다.
 */
fun careNudgeCandidates(cycles: List<CareCycleProgress>): List<CareCycleProgress> =
    cycles.filter { it.unit == CareCycleUnit.WASH_COUNT && it.isOverdue }

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()

private fun String.toShortDateText(): String =
    toLocalDateOrNull()?.toShortDateText() ?: this

private fun LocalDate.toShortDateText(): String = "${monthValue}월 ${dayOfMonth}일"
