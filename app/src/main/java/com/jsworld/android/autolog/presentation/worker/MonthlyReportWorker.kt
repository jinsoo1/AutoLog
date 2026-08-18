package com.jsworld.android.autolog.presentation.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.core.util.Constant.ACTION_OPEN_REPORT
import com.jsworld.android.autolog.domain.model.MonthlyReportNotice
import com.jsworld.android.autolog.domain.model.buildMonthlyReportNotice
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.ExpenseReportRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.activity.MainActivity
import com.jsworld.android.autolog.presentation.scheduler.MonthlyReportScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.NumberFormat
import java.time.YearMonth
import kotlinx.coroutines.flow.first

/**
 * 매월 1일 지난달 지출을 집계해 "N월 리포트가 준비됐어요"를 보낸다.
 *
 * - 집계는 리포트 탭과 같은 소스(ExpenseReportRepository) — 알림 숫자와
 *   탭에서 보는 숫자가 다르면 신뢰가 깨진다.
 * - 지난달에 기록이 없으면 조용히 넘어간다(0원 리포트는 소음).
 * - 알림은 앱 전체 1건. 차량이 여럿이면 본문에 차량별 줄을 담는다.
 */
@HiltWorker
class MonthlyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val carRepository: CarRepository,
    private val expenseReportRepository: ExpenseReportRepository,
    private val userPrefsRepository: UserPrefsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val forceTest = inputData.getBoolean(KEY_FORCE_TEST, false)

        // 설정 읽기가 실패해도 체인은 살린다 — 이 한 번의 실패로 예약이 끊기면
        // 앱을 다시 열 때까지 알림이 영구히 멈춘다.
        val enabled = runCatching {
            userPrefsRepository.observeMonthlyReportNotificationEnabled().first()
        }.getOrElse { e ->
            android.util.Log.e(TAG, "prefs read failed — 다음 달 재예약", e)
            if (!forceTest) MonthlyReportScheduler.scheduleNextFromWorker(applicationContext)
            return Result.success()
        }

        // 꺼져 있으면 체인을 세운다(끌 때 cancel 되지만 경합 대비 안전망).
        if (!enabled && !forceTest) return Result.success()

        // 집계보다 예약 먼저 — 도중에 예외가 나도 다음 달 체인이 살아야 한다.
        // 테스트 실행은 월간 체인을 건드리지 않는다.
        if (!forceTest) {
            MonthlyReportScheduler.scheduleNextFromWorker(applicationContext)
        }

        runCatching { checkAndNotify(forceTest) }
            .onFailure { android.util.Log.e(TAG, "notify failed", it) }

        return Result.success()
    }

    private suspend fun checkAndNotify(forceTest: Boolean) {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return

        val lastMonth = YearMonth.now().minusMonths(1)
        val cars = carRepository.getAllCars().first()

        val notice = buildMonthlyReportNotice(
            month = lastMonth,
            carExpenses = cars.map { car ->
                car.name to expenseReportRepository.observeMonthlyExpenses(car.id).first()
            }
        ) ?: run {
            android.util.Log.d(TAG, "skip — ${lastMonth}에 알릴 기록 없음")
            // 테스트인데 조용히 끝나면 고장난 것처럼 보인다 — 왜 없는지 알려준다.
            if (forceTest) showTestResultNotification(lastMonth)
            return
        }

        showNotification(notice)
    }

    /** 테스트 실행 전용 결과 안내 — 정기 알림 경로에서는 호출되지 않는다 */
    private fun showTestResultNotification(lastMonth: YearMonth) {
        val reason = "지난달(${lastMonth.monthValue}월)에 지출 기록이 없어 정기 실행에서도 " +
            "알림이 가지 않아요. 지난달 날짜로 주유나 정비 기록을 하나 넣고 다시 테스트해보세요."

        val notification = NotificationCompat.Builder(
            applicationContext,
            AutoLogNotificationHelper.MONTHLY_REPORT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentTitle("테스트: 보낼 리포트가 없어요")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(TEST_RESULT_NOTIFICATION_ID, notification)
    }

    private fun showNotification(notice: MonthlyReportNotice) {
        val context = applicationContext

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_REPORT
            data = Uri.parse("autolog://report")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val won = NumberFormat.getIntegerInstance()
        val title = "${notice.month.monthValue}월 리포트가 준비됐어요"
        val body = when {
            // 기록은 있는데 전부 금액 미입력 — 0원이라고 말하면 거짓말이 된다
            notice.total == 0L ->
                "지난달 기록 ${notice.missingCostCount}건에 금액이 입력되지 않았어요"
            notice.missingCostCount > 0 ->
                "지난달 총 ${won.format(notice.total)}원 · 금액 미입력 ${notice.missingCostCount}건 제외"
            else ->
                "지난달 총 ${won.format(notice.total)}원 — 어디에 얼마 썼는지 확인해보세요"
        }

        val builder = NotificationCompat.Builder(
            context,
            AutoLogNotificationHelper.MONTHLY_REPORT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (notice.lines.isEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        } else {
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            notice.lines.forEach { style.addLine("${it.carName} — ${won.format(it.total)}원") }
            builder.setStyle(style)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        /** 테스트용 inputData 키 — 디버그 전용 테스트 버튼(enqueueTest)에서만 쓰인다 */
        const val KEY_FORCE_TEST = "force_test"

        private const val TAG = "MonthlyReport"

        // 주간(1001)·정비(2000+carId) 대역과 겹치지 않게
        private const val NOTIFICATION_ID = 4000
        private const val REQUEST_CODE = 4100
        private const val TEST_RESULT_NOTIFICATION_ID = 4999
    }
}
