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

    /** 위젯을 추가했을 때 — 기존 예약을 오늘 기준으로 다시 건다 */
    fun schedule(context: Context) {
        enqueue(context, ExistingWorkPolicy.REPLACE)
    }

    /**
     * 워커 안에서의 내일 예약 — **REPLACE 를 쓰면 안 된다.**
     *
     * REPLACE 는 같은 이름의 기존 작업을 취소하는데, 그 기존 작업이 지금 **실행 중인
     * 자기 자신**이다. 자신을 취소하면서 새 예약을 거는 셈이라 타이밍에 따라 체인이
     * 통째로 사라진다 — 그러면 위젯은 앱을 열기 전까지 하루 지난 값을 그대로 띄운다.
     *
     * KEEP 도 답이 아니다. 실행 중인 자신을 "살아있는 예약"으로 보고 새 요청을 버린다.
     * APPEND_OR_REPLACE 만이 실행 중인 자신 **뒤에 이어 붙는다.**
     * (1.2.2 에서 알림 체인이 하루 만에 끊겼던 것과 같은 문제다)
     */
    fun scheduleNextFromWorker(context: Context) {
        enqueue(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val next = now.toLocalDate().plusDays(1).atTime(0, 5).atZone(now.zone) // 00:05
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(1)

        val req = OneTimeWorkRequestBuilder<DailyWidgetRefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(UNIQUE_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, policy, req)
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