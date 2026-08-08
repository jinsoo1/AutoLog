package com.jsworld.android.autolog.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WidgetDailyUpdateScheduler {

    private const val UNIQUE_WORK = "daily_widget_refresh"

    fun schedule(context: Context) {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val next = now.toLocalDate().plusDays(1).atTime(0, 5).atZone(now.zone) // 00:05
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(1)

        val req = OneTimeWorkRequestBuilder<DailyWidgetRefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(UNIQUE_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    /**
     * 앱을 열 때마다 호출해 체인을 되살린다.
     *
     * 자기 재예약 체인은 강제 종료·OS 정리로 끊길 수 있는데, 끊기면
     * 위젯을 다시 추가하기 전까지 복구할 곳이 없다. 위젯이 실제로 있을 때만
     * 다시 예약한다(REPLACE 라 다음 00:05 로 재계산될 뿐, 중복 실행은 없다).
     */
    suspend fun ensureScheduled(context: Context) {
        val hasWidgets = runCatching {
            GlanceAppWidgetManager(context)
                .getGlanceIds(CarStatusWidget::class.java)
                .isNotEmpty()
        }.getOrDefault(false)

        if (hasWidgets) schedule(context)
    }
}