package com.jsworld.android.autolog.presentation.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.jsworld.android.autolog.domain.model.nextMonthlyReportTime
import com.jsworld.android.autolog.presentation.worker.MonthlyReportWorker
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * 월간 리포트 도착 알림 예약 — 매월 1일 오전 9시.
 *
 * 정비 알림과 같은 자기 재예약(one-time) 방식이지만, 워커 안에서의 재예약은
 * KEEP 이 아니라 **APPEND_OR_REPLACE** 를 쓴다. KEEP 은 "실행 중인 자기 자신"을
 * 살아있는 예약으로 보고 새 요청을 버리므로 체인이 앱 재시작 복구에만 의존하게 된다 —
 * 한 달에 한 번뿐인 이 알림에서 그건 사실상 알림이 안 오는 것과 같다.
 */
object MonthlyReportScheduler {

    private const val UNIQUE_WORK_NAME = "monthly_report_notice"
    private const val TEST_WORK_NAME = "monthly_report_notice_test"

    /** 매월 1일 오전 9시 — 출근길에 보는 시간대. 설정으로 빼지 않는다(월 1회에 시간 옵션은 과함) */
    const val HOUR = 9

    /** 앱 시작 복구용 — 살아있는 예약이 있으면 유지 */
    fun scheduleNext(context: Context) {
        enqueue(context, ExistingWorkPolicy.KEEP)
    }

    /** 알림을 켤 때 — 기존 예약을 교체 */
    fun reschedule(context: Context) {
        enqueue(context, ExistingWorkPolicy.REPLACE)
    }

    /**
     * 워커 안에서의 다음 달 예약. 실행 중인 자신 뒤에 이어 붙는다 —
     * 자신을 취소하지도(REPLACE), 무시되지도(KEEP) 않는 유일한 정책.
     */
    fun scheduleNextFromWorker(context: Context) {
        enqueue(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(TEST_WORK_NAME)
    }

    /**
     * 테스트용 — 설정 화면의 디버그 전용 버튼에서만 호출된다(BuildConfig.DEBUG 가드).
     * 10초 뒤 지난달 집계로 알림을 보내본다. 월간 체인은 건드리지 않는다.
     */
    fun enqueueTest(context: Context) {
        val request = OneTimeWorkRequestBuilder<MonthlyReportWorker>()
            .setInputData(workDataOf(MonthlyReportWorker.KEY_FORCE_TEST to true))
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(TEST_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun enqueue(context: Context, policy: ExistingWorkPolicy) {
        val now = ZonedDateTime.now()
        val delay = Duration.between(now, nextMonthlyReportTime(now, HOUR))
            .toMillis().coerceAtLeast(1)

        val request = OneTimeWorkRequestBuilder<MonthlyReportWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request)
    }
}
