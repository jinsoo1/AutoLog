package com.jsworld.android.autolog.core.util

import android.content.Context
import com.jsworld.android.autolog.domain.repository.CarScheduleRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.scheduler.MaintenanceAlertScheduler
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 앱 시작 시 하루 1회 알림 체인을 복구한다.
 * 기기 재부팅·강제 종료·앱 업데이트로 WorkManager 예약이 사라졌을 수 있어서다.
 * KEEP 정책이라 살아 있는 예약이 있으면 건드리지 않는다.
 *
 * ⚠️ 이 체인은 정비 알림만의 것이 아니다 — **날짜 일정 알림도 같은 워커에 얹혀 있다.**
 * 정비 알림은 기본 꺼짐이고 일정 알림은 기본 켜짐이라, 정비 알림 기준으로만
 * 예약하면 일정을 등록해도 알림이 영영 오지 않는다(설정은 켜져 있는데 조용한 상태).
 * 그래서 둘 중 **하나라도** 켜져 있으면 예약한다.
 */
@Singleton
class MaintenanceAlertStartupManager @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val scheduleRepository: CarScheduleRepository
) {
    suspend fun restoreIfNeeded(context: Context) {
        val prefs = userPrefsRepository.observeMaintenanceAlertPrefs().first()
        val scheduleAlertOn = userPrefsRepository.observeScheduleAlertEnabled().first()

        // 일정 알림은 등록된 일정이 있을 때만 의미가 있다 — 빈 상태에서 워커를
        // 돌려봐야 매일 아무것도 안 하고 끝난다(배터리만 쓴다).
        val hasSchedules = scheduleAlertOn &&
            runCatching { scheduleRepository.getAll().isNotEmpty() }.getOrDefault(false)

        if (prefs.enabled || hasSchedules) {
            AutoLogNotificationHelper.createChannels(context)
            MaintenanceAlertScheduler.scheduleNext(context, prefs.hour)
        }
    }
}
