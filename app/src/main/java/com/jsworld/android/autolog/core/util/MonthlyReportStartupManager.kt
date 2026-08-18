package com.jsworld.android.autolog.core.util

import android.content.Context
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.scheduler.MonthlyReportScheduler
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 앱 시작 시 월간 리포트 알림 체인을 복구한다.
 * 기본 켜짐이므로 업데이트 직후 첫 실행에서 여기서 처음 예약된다.
 * KEEP 정책이라 살아 있는 예약이 있으면 건드리지 않는다.
 */
@Singleton
class MonthlyReportStartupManager @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {
    suspend fun restoreIfNeeded(context: Context) {
        if (userPrefsRepository.observeMonthlyReportNotificationEnabled().first()) {
            AutoLogNotificationHelper.createChannels(context)
            MonthlyReportScheduler.scheduleNext(context)
        }
    }
}
