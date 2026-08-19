package com.jsworld.android.autolog.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

/**
 * 정비 시기 예측 — "이 페이스면 11월쯤 엔진오일".
 *
 * 새 데이터를 만들지 않는다. 리포트가 이미 계산하는 월별 주행거리와,
 * 정비 상태가 이미 계산하는 남은 거리·남은 일수를 곱셈 한 번으로 잇는 게 전부다.
 *
 * 원칙: 계산할 수 없으면 null. 관측이 부족한데 그럴싸한 날짜를 내놓는 건
 * 이 기능에서 가장 하면 안 되는 일이다(사용자가 그 날짜를 믿고 정비를 미룬다).
 */

/** 예측의 근거 — 거리가 먼저 오는지, 기간이 먼저 오는지 */
enum class PredictionBasis { DISTANCE, PERIOD }

/** 내 주행 페이스. [monthsUsed] 는 평균에 쓴 달 수(문구에 밝힌다) */
data class DrivingPace(val monthlyKm: Int, val monthsUsed: Int)

data class MaintenancePrediction(
    val settingId: Long,
    val name: String,
    val date: LocalDate,
    val basis: PredictionBasis
)

/** 평균을 내려면 완전한 달이 최소 이만큼 필요하다 — 한 달만으로는 페이스라고 부를 수 없다 */
const val PACE_MIN_MONTHS = 2

/** 평균에 쓸 최근 달 수 — 계절 편차를 흡수할 만큼만, 오래된 습관은 빼고 */
const val PACE_WINDOW_MONTHS = 3

/**
 * 최근 주행 페이스. [current] 달은 **아직 진행 중이라 제외**한다 —
 * 8월 3일에 8월을 넣으면 "월 200km 밖에 안 타는 사람"이 되어버린다.
 *
 * @return 완전한 달이 [PACE_MIN_MONTHS] 개 미만이면 null
 */
fun estimateDrivingPace(
    expenses: List<MonthlyExpense>,
    current: YearMonth,
    windowMonths: Int = PACE_WINDOW_MONTHS
): DrivingPace? {
    val usable = expenses
        .filter { it.month < current }
        .sortedByDescending { it.month }
        .take(windowMonths)
        .mapNotNull { it.drivenKm }
        .filter { it > 0 }

    if (usable.size < PACE_MIN_MONTHS) return null

    return DrivingPace(
        monthlyKm = usable.sum() / usable.size,
        monthsUsed = usable.size
    )
}

/**
 * 항목 하나의 예상 시점. 거리·기간 중 **먼저 오는 쪽**을 쓴다(상태 판정과 같은 기준).
 *
 * @return 계산할 근거가 없으면 null
 */
fun predictMaintenanceDate(
    remainingKm: Int?,
    remainingDays: Long?,
    pace: DrivingPace?,
    today: LocalDate
): Pair<LocalDate, PredictionBasis>? {
    val byDistance = if (remainingKm != null && pace != null && pace.monthlyKm > 0) {
        val daysPerKm = 30.0 / pace.monthlyKm
        val days = ceil(remainingKm * daysPerKm).toLong()
        // 페이스가 아주 느리면 몇십 년 뒤가 나온다 — 그건 예측이 아니라 소음이다.
        if (days > MAX_PREDICT_DAYS) null else today.plusDays(days)
    } else null

    val byPeriod = remainingDays?.let { today.plusDays(it) }

    return when {
        byDistance != null && byPeriod != null ->
            if (byDistance <= byPeriod) byDistance to PredictionBasis.DISTANCE
            else byPeriod to PredictionBasis.PERIOD
        byDistance != null -> byDistance to PredictionBasis.DISTANCE
        byPeriod != null -> byPeriod to PredictionBasis.PERIOD
        else -> null
    }
}

/** 10년 뒤 예상은 아무 도움이 안 된다 */
private const val MAX_PREDICT_DAYS = 3650L

/**
 * 예측 목록(가까운 순). 다음 항목은 제외한다:
 * - **기록이 없는 항목** — 0km·오늘 기준이라 계산이 거짓이 된다(앱 전체의 hasHistory 규칙)
 * - **이미 지난 항목** — 예측이 아니라 현재 상태이고, '다가오는 지출' 카드가 이미 다룬다
 */
fun buildMaintenancePredictions(
    items: List<MaintenanceUiModel>,
    pace: DrivingPace?,
    today: LocalDate,
    limit: Int = 3
): List<MaintenancePrediction> =
    items
        .asSequence()
        .filter { it.hasHistory }
        .filter { (it.remainingKm ?: 1) > 0 && (it.remainingDays ?: 1L) > 0L }
        .mapNotNull { item ->
            predictMaintenanceDate(item.remainingKm, item.remainingDays, pace, today)
                ?.let { (date, basis) ->
                    MaintenancePrediction(item.settingId, item.name, date, basis)
                }
        }
        .sortedBy { it.date }
        .take(limit)
        .toList()

/**
 * "11월 중순" / "내년 6월" / "2029년 3월".
 *
 * 예측은 어차피 근사라서 날짜까지 쓰지 않는다 — 초·중순·말이 정직한 해상도이고,
 * 해가 바뀌면 그마저 과한 정밀도라 달만 말한다.
 */
fun predictedDateLabel(date: LocalDate, today: LocalDate): String {
    val month = date.monthValue
    return when (date.year) {
        today.year -> "${month}월 ${tenthLabel(date.dayOfMonth)}"
        today.year + 1 -> "내년 ${month}월"
        else -> "${date.year}년 ${month}월"
    }
}

private fun tenthLabel(dayOfMonth: Int): String = when {
    dayOfMonth <= 10 -> "초"
    dayOfMonth <= 20 -> "중순"
    else -> "말"
}

/** 예측 근거 한 마디 — 왜 이 날짜인지 밝힌다 */
fun predictionBasisLabel(prediction: MaintenancePrediction, item: MaintenanceUiModel?): String {
    val remain = when (prediction.basis) {
        PredictionBasis.DISTANCE -> item?.remainingKm?.let { "${formatKmForPrediction(it)}km 남음" }
        PredictionBasis.PERIOD -> item?.remainingDays?.let { "${it}일 남음" }
    }
    val basis = when (prediction.basis) {
        PredictionBasis.DISTANCE -> "거리 기준"
        PredictionBasis.PERIOD -> "기간이 먼저 와요"
    }
    return listOfNotNull(remain, basis).joinToString(" · ")
}

private fun formatKmForPrediction(km: Int): String =
    java.text.NumberFormat.getIntegerInstance().format(km)

/** 남은 개월 수로 '다음 정비까지' 대략 몇 달인지 — 카드 부제용 */
fun monthsUntil(date: LocalDate, today: LocalDate): Long =
    ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(date))
