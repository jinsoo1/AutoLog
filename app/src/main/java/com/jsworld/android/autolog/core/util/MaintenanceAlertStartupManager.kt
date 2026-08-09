package com.jsworld.android.autolog.core.util

import android.content.Context
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.scheduler.MaintenanceAlertScheduler
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 앱 시작 시 정비 알림 체인을 복구한다.
 * 기기 재부팅·강제 종료·앱 업데이트로 WorkManager 예약이 사라졌을 수 있어서다.
 * KEEP 정책이라 살아 있는 예약이 있으면 건드리지 않는다.
 */
@Singleton
class MaintenanceAlertStartupManager @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {
    suspend fun restoreIfNeeded(context: Context) {
        val prefs = userPrefsRepository.observeMaintenanceAlertPrefs().first()
        if (prefs.enabled) {
            AutoLogNotificationHelper.createChannels(context)
            MaintenanceAlertScheduler.scheduleNext(context, prefs.hour)
        }
    }
}
