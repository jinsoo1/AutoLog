package com.jsworld.android.autolog.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // ⚠️ 다음 날 예약을 갱신보다 먼저 건다.
        // 갱신 도중 예외가 나도 체인이 끊기지 않아야 한다 — 이 체인이 죽으면
        // 위젯을 다시 추가하기 전까지 되살릴 곳이 없다(OS 주기 갱신은 꺼져 있음).
        WidgetDailyUpdateScheduler.schedule(applicationContext)

        runCatching { CarStatusWidgetUpdater.updateAll(applicationContext) }

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