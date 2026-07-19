package com.jsworld.android.autolog.presentation.widget

import android.content.Context
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
}