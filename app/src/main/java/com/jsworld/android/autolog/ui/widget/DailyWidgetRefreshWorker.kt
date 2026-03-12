package com.jsworld.android.autolog.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // ✅ 전체 위젯 갱신
        CarStatusWidgetUpdater.updateAll(applicationContext)

        // ✅ 다음날 것도 다시 예약 (매일 반복)
        WidgetDailyUpdateScheduler.schedule(applicationContext)

        return Result.success()
    }
}

object CarStatusWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(CarStatusWidget::class.java)
        ids.forEach { glanceId ->
            CarStatusWidget().update(context, glanceId)
        }
    }
}