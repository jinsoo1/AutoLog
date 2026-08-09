package com.jsworld.android.autolog.presentation.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jsworld.android.autolog.presentation.worker.MaintenanceAlertWorker
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * 정비 임박/초과 알림의 일일 검사 예약.
 *
 * PeriodicWork 대신 자기 재예약(one-time) 방식 — 위젯 일일 갱신과 같은 이유로,
 * "매일 정확히 그 시각"에 돌려면 다음 실행 시각을 직접 계산해 거는 편이 낫다.
 * 워커는 갱신보다 예약을 먼저 걸어 체인이 끊기지 않게 한다.
 */
object MaintenanceAlertScheduler {

    private const val UNIQUE_WORK_NAME = "maintenance_alert_daily"

    /** 이미 예약이 있으면 유지(KEEP). 워커의 자기 재예약, 앱 시작 복구용 */
    fun scheduleNext(context: Context, hour: Int) {
        enqueue(context, hour, ExistingWorkPolicy.KEEP)
    }

    /** 알림 켜기·시간 변경 시 — 기존 예약을 새 시각으로 교체 */
    fun reschedule(context: Context, hour: Int) {
        enqueue(context, hour, ExistingWorkPolicy.REPLACE)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun enqueue(context: Context, hour: Int, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<MaintenanceAlertWorker>()
            .setInitialDelay(delayUntilNext(hour).toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request)
    }

    private fun delayUntilNext(hour: Int): Duration {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
