package com.jsworld.android.autolog.ui.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jsworld.android.autolog.ui.worker.WeeklyMileageReminderWorker
import java.util.concurrent.TimeUnit

object WeeklyMileageWorkScheduler {

    private const val UNIQUE_WORK_NAME = "weekly_mileage_reminder_once"
    private const val TEST_WORK_NAME = "weekly_mileage_reminder_test"

    fun enqueueNext(context: Context) {
        val delay = calculateDelayUntilNextSunday8Pm()

        val request = OneTimeWorkRequestBuilder<WeeklyMileageReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueTest(context: Context) {
        android.util.Log.d("WeeklyWorker", "enqueueTest called")

        val request = OneTimeWorkRequestBuilder<WeeklyMileageReminderWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .addTag("weekly_test")
            .build()

        val wm = WorkManager.getInstance(context)
        android.util.Log.d("WeeklyWorker", "WorkManager instance acquired")

        wm.enqueueUniqueWork(
            "weekly_mileage_reminder_test",
            ExistingWorkPolicy.REPLACE,
            request
        )

        android.util.Log.d("WeeklyWorker", "work enqueued id=${request.id}")

        wm.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            android.util.Log.d(
                "WeeklyWorker",
                "work state=${info?.state}, id=${info?.id}"
            )
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(TEST_WORK_NAME)
    }

    private fun calculateDelayUntilNextSunday8Pm(): Long {
        val zoneId = java.time.ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zoneId)

        var next = now
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
            .withHour(20)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusWeeks(1)
        }

        return java.time.Duration.between(now, next).toMillis()
    }
}