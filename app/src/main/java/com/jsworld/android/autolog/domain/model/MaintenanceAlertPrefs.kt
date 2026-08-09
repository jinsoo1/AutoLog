package com.jsworld.android.autolog.domain.model

/**
 * 정비 임박/초과 푸시 알림 설정.
 *
 * 알림 대상은 별도 목록이 아니라 "주기가 있는 활성 정비 항목 전부"다.
 * 수리·세차처럼 주기가 없는 항목은 상태 계산에서 빠지므로 알림도 없다.
 */
data class MaintenanceAlertPrefs(
    /** 마스터 스위치. 꺼지면 아래 값과 무관하게 알림이 없다. */
    val enabled: Boolean = false,
    /** SOON 진입 알림 */
    val soonEnabled: Boolean = true,
    /** OVERDUE 진입 알림 */
    val overdueEnabled: Boolean = true,
    /** 하루 1회 검사 시각(0~23시) */
    val hour: Int = DEFAULT_HOUR,
    /** 초과 상태 지속 시 재알림 주기(일). 0 = 안 함 */
    val remindDays: Int = 0
) {
    companion object {
        const val DEFAULT_HOUR = 9
        /** 초과 리마인드 선택지: 안 함 / 7일 / 14일 */
        val REMIND_OPTIONS = listOf(0, 7, 14)

        fun remindLabel(days: Int): String = if (days == 0) "안 함" else "${days}일마다"
    }
}

/**
 * 항목(settingId)별 마지막으로 알림을 보낸 상태.
 * 같은 상태로 머무는 동안 다시 알리지 않기 위한 기록 — 상태가 바뀌거나(전이),
 * 초과 리마인드 주기가 지났을 때만 새 알림을 보낸다.
 */
data class MaintenanceAlertNotifiedState(
    val status: String,
    val notifiedAt: Long
)

/**
 * 이 항목에 대해 "지금" 알림을 보내야 하는가 — 워커의 스팸 방지 핵심 규칙.
 *
 * - 처음 보는 상태(기록 없음)나 상태 전이(SOON↔OVERDUE): 알린다.
 * - 같은 상태 지속: 침묵. 단 OVERDUE 는 리마인드 주기가 지났으면 다시 알린다.
 */
fun shouldNotifyMaintenanceAlert(
    status: MaintenanceStatus,
    prev: MaintenanceAlertNotifiedState?,
    remindDays: Int,
    now: Long
): Boolean = when {
    prev == null || prev.status != status.name -> true
    status == MaintenanceStatus.OVERDUE && remindDays > 0 &&
        now - prev.notifiedAt >= remindDays * DAY_MILLIS -> true
    else -> false
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
