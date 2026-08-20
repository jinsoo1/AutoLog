package com.jsworld.android.autolog.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 날짜 기반 일정. 전부 순수 계산 — 정기검사 제안·D-day·완료 처리 규칙을
 * 테스트로 잠근다 (틀리면 과태료로 이어지는 숫자들이다).
 */

enum class ScheduleType {
    /** 자동차 정기검사 — 연식으로 제안하되 사용자가 등록증 보고 수정 */
    INSPECTION,

    /** 보험 만기 */
    INSURANCE,

    /** 자동차세 — 기본 12월 16일·6개월 반복(연납이면 날짜를 1월로 바꾸면 됨) */
    TAX,

    /** 직접 추가 (엔진 경고등 재점검 등) */
    CUSTOM;

    companion object {
        fun from(name: String): ScheduleType =
            entries.firstOrNull { it.name == name } ?: CUSTOM
    }
}

data class CarSchedule(
    val id: Long,
    val carId: Long,
    val type: ScheduleType,
    val title: String,
    /** yyyy-MM-dd */
    val dueDate: String,
    val repeatMonths: Int?,
    val memo: String?
) {
    fun dueLocalDate(): LocalDate? = runCatching { LocalDate.parse(dueDate) }.getOrNull()

    /** 남은 일수. 음수 = 지남. 날짜가 깨져 있으면 null */
    fun remainingDays(today: LocalDate): Long? =
        dueLocalDate()?.let { ChronoUnit.DAYS.between(today, it) }
}

/** 프리셋 기본 반복 주기(개월) */
const val REPEAT_INSPECTION = 24
const val REPEAT_INSURANCE = 12
const val REPEAT_TAX = 6

/**
 * 정기검사 도래일 **제안** — 비사업용 승용: 최초 등록 후 4년, 이후 2년 주기.
 *
 * 차량에는 연식(연도)만 있고 법정 기준인 최초 등록일(연월일)은 없다.
 * 그래서 연식 기준으로 계산한 값은 확정이 아니라 제안이고, 추가 화면에서
 * 사용자가 등록증의 검사 유효기간으로 고치는 걸 전제한다.
 *
 * @param yearText CarEntity.year — "2020" 같은 문자열. 파싱 실패 시 null
 * @return 오늘 이후 첫 도래일(연식+4년에서 2년씩 굴린 값). 연식을 모르면 null
 */
fun suggestInspectionDate(yearText: String?, today: LocalDate): LocalDate? {
    val year = yearText?.trim()?.take(4)?.toIntOrNull() ?: return null
    if (year < 1980 || year > today.year + 1) return null // 오타 방어

    // 등록일을 모르므로 연식 연도의 중간(7월 1일)을 기준으로 잡는다 —
    // 최대 ±6개월 오차인데, 어차피 제안값이고 사용자가 고친다.
    var due = LocalDate.of(year, 7, 1).plusYears(4)
    while (due.isBefore(today)) due = due.plusYears(2)
    return due
}

/** 자동차세 다음 납부일 제안 — 6월 16일 / 12월 16일 중 오늘 이후 첫 날짜 */
fun suggestTaxDate(today: LocalDate): LocalDate {
    val june = LocalDate.of(today.year, 6, 16)
    val december = LocalDate.of(today.year, 12, 16)
    return when {
        !june.isBefore(today) -> june
        !december.isBefore(today) -> december
        else -> LocalDate.of(today.year + 1, 6, 16)
    }
}

/**
 * 완료 처리 후의 다음 도래일. 반복이 없으면 null(= 일정 삭제).
 *
 * 기준은 오늘이 아니라 **원래 도래일**이다 — 12월 16일 자동차세를 12월 20일에
 * 완료해도 다음은 6월 16일이어야지, 6월 20일이 되면 날짜가 밀려간다.
 * 단, 더한 결과가 여전히 과거면 오늘 이후가 될 때까지 굴린다(오래 방치한 일정).
 */
fun nextDueDateAfterDone(schedule: CarSchedule, today: LocalDate): LocalDate? {
    val repeat = schedule.repeatMonths ?: return null
    if (repeat <= 0) return null
    var next = (schedule.dueLocalDate() ?: today).plusMonths(repeat.toLong())
    while (!next.isAfter(today)) next = next.plusMonths(repeat.toLong())
    return next
}

/** 목록 정렬 — 가까운 순, 날짜가 깨진 행은 맨 뒤 */
fun sortSchedules(schedules: List<CarSchedule>, today: LocalDate): List<CarSchedule> =
    schedules.sortedBy { it.remainingDays(today) ?: Long.MAX_VALUE }

/**
 * 홈에 꺼내 보일 창(일). **행동해야 할 때**만 뜨게 한다 —
 * 늘 떠 있으면 배경이 되고, 배경이 되면 안 보인다.
 */
const val SCHEDULE_HOME_DAYS = 30L

/**
 * 리포트에 꺼내 보일 창(일). 홈보다 넓다 — 리포트는 지금 할 일이 아니라
 * **돈 계획**을 보는 자리라, 다음 달에 나갈 것까지 보여야 쓸모가 있다.
 */
const val SCHEDULE_REPORT_DAYS = 60L

/**
 * 곧 도래하는 일정(가까운 순). **이미 지난 것도 포함**한다 —
 * 정기검사는 지나도 사라지지 않고, 지났다는 사실이 가장 중요한 정보다.
 */
fun upcomingSchedules(
    schedules: List<CarSchedule>,
    today: LocalDate,
    withinDays: Long
): List<CarSchedule> = sortSchedules(
    schedules.filter { schedule ->
        val remaining = schedule.remainingDays(today)
        remaining != null && remaining <= withinDays
    },
    today
)

/** "D-86" / "D-DAY" / "D+3" */
fun dDayLabel(remainingDays: Long): String = when {
    remainingDays > 0 -> "D-$remainingDays"
    remainingDays == 0L -> "D-DAY"
    else -> "D+${-remainingDays}"
}

/* ───────────────────────── 알림 ───────────────────────── */

/**
 * 일정 알림 단계. 정비 알림과 같은 원칙 — **단계가 바뀔 때만** 알린다.
 * 매일 "D-13일 남았어요"를 보내면 알림을 꺼버리게 된다.
 */
enum class ScheduleAlertStage {
    /** 아직 멀었다 — 알리지 않는다 */
    FAR,

    /** 2주 전 */
    TWO_WEEKS,

    /** 1주 전 */
    ONE_WEEK,

    /** 당일 */
    TODAY,

    /** 지났다 */
    OVERDUE
}

/** 남은 일수 → 단계 */
fun scheduleAlertStage(remainingDays: Long): ScheduleAlertStage = when {
    remainingDays < 0 -> ScheduleAlertStage.OVERDUE
    remainingDays == 0L -> ScheduleAlertStage.TODAY
    remainingDays <= 7 -> ScheduleAlertStage.ONE_WEEK
    remainingDays <= 14 -> ScheduleAlertStage.TWO_WEEKS
    else -> ScheduleAlertStage.FAR
}

/**
 * 지금 알려야 하는가. FAR 은 알리지 않고, 나머지는 **직전 단계와 다를 때만**.
 *
 * 되돌아가는 전이(사용자가 날짜를 미뤘을 때)에도 알리지 않는다 —
 * 사용자가 방금 그 날짜를 직접 바꿨으니 알려줄 필요가 없다.
 */
fun shouldNotifySchedule(stage: ScheduleAlertStage, previous: ScheduleAlertStage?): Boolean {
    if (stage == ScheduleAlertStage.FAR) return false
    if (previous == null) return true
    return stage.ordinal > previous.ordinal
}

/** 알림 본문 — 남은 일수를 사람 말로. 제목 뒤에 조사가 붙으므로 띄우지 않는다 */
fun scheduleAlertText(title: String, remainingDays: Long): String = when {
    remainingDays < 0 -> "$title 날짜가 ${-remainingDays}일 지났어요"
    remainingDays == 0L -> "오늘이 $title 날짜예요"
    remainingDays == 1L -> "내일이 $title 날짜예요"
    else -> "${title}까지 ${remainingDays}일 남았어요"
}

/**
 * 알림·목록에 쓰는 날짜 표기 — **연도를 반드시 포함한다.**
 * 일정은 몇 년 뒤가 흔해서 연도가 없으면 지난 날짜처럼 읽힌다.
 */
fun formatScheduleDate(dueDate: String, today: LocalDate): String = runCatching {
    val date = LocalDate.parse(dueDate)
    if (date.year == today.year) "${date.monthValue}월 ${date.dayOfMonth}일"
    else "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"
}.getOrDefault(dueDate)
