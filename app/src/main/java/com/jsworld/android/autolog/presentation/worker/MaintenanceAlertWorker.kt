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
import com.jsworld.android.autolog.core.util.Constant.ACTION_OPEN_CAR_DETAIL
import com.jsworld.android.autolog.core.util.Constant.EXTRA_CAR_ID
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
import com.jsworld.android.autolog.domain.model.MaintenanceStatus
import com.jsworld.android.autolog.domain.model.MaintenanceUiModel
import com.jsworld.android.autolog.domain.model.shouldNotifyMaintenanceAlert
import com.jsworld.android.autolog.domain.repository.CarMaintenanceRepository
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.activity.MainActivity
import com.jsworld.android.autolog.presentation.scheduler.MaintenanceAlertScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 하루 1회 모든 차량의 정비 상태를 검사해 임박/초과 알림을 보낸다.
 *
 * 스팸 방지 규칙:
 * - 같은 항목은 상태가 "바뀔 때"만 알린다 (SOON 진입 1회, OVERDUE 진입 1회).
 * - 초과 리마인드를 켰다면 OVERDUE 지속 중에도 설정 주기마다 다시 알린다.
 * - 기록이 하나도 없는 항목은 제외 — 0km/오늘 기준 계산이라 켜자마자
 *   초과로 뜨는데, 이걸 알림으로 쏟아내면 안 된다.
 * - 차량당 알림 1건. 여러 항목이면 목록으로 묶는다.
 */
@HiltWorker
class MaintenanceAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val carRepository: CarRepository,
    private val carMaintenanceRepository: CarMaintenanceRepository,
    private val userPrefsRepository: UserPrefsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = userPrefsRepository.observeMaintenanceAlertPrefs().first()
        val forceTest = inputData.getBoolean(KEY_FORCE_TEST, false)

        // 꺼져 있으면 체인도 세운다(끌 때 cancel 되지만 경합 대비 안전망).
        if (!prefs.enabled) {
            android.util.Log.d(TAG, "doWork skipped — alert disabled (forceTest=$forceTest)")
            return Result.success()
        }

        // ⚠️ 검사보다 예약을 먼저 — 도중에 예외가 나도 내일 체인이 살아야 한다.
        // 테스트 실행은 일일 체인을 건드리지 않는다.
        if (!forceTest) {
            MaintenanceAlertScheduler.scheduleNext(applicationContext, prefs.hour)
        }

        runCatching { checkAndNotify(prefs, forceTest) }

        return Result.success()
    }

    private suspend fun checkAndNotify(prefs: MaintenanceAlertPrefs, forceTest: Boolean) {
        val allowed = NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        android.util.Log.d(TAG, "run forceTest=$forceTest allowed=$allowed prefs=$prefs")
        if (!allowed) return

        val now = System.currentTimeMillis()
        val notified = userPrefsRepository.getMaintenanceAlertNotifiedStates()
        val cars = carRepository.getAllCars().first()
        val currentIds = mutableSetOf<Long>()

        var sent = 0
        var urgentTotal = 0
        var noHistoryCount = 0

        cars.forEach { car ->
            val urgent = carMaintenanceRepository.observeMaintenanceStatusList(car.id).first()
            urgentTotal += urgent.size

            val withHistory = urgent.filter { it.hasHistory }
            noHistoryCount += urgent.size - withHistory.size

            val items = withHistory.filter {
                (it.status == MaintenanceStatus.SOON && prefs.soonEnabled) ||
                    (it.status == MaintenanceStatus.OVERDUE && prefs.overdueEnabled)
            }
            android.util.Log.d(
                TAG,
                "car=${car.name} urgent=${urgent.size} withHistory=${withHistory.size} eligible=${items.size}"
            )
            currentIds += items.map { it.settingId }

            // 새로 알릴 거리가 있는지 — 전이했거나, 초과 리마인드 주기가 지났거나.
            // 테스트 실행은 검사 없이 전부 알린다.
            val fresh = if (forceTest) items else items.filter { item ->
                shouldNotifyMaintenanceAlert(
                    status = item.status,
                    prev = notified[item.settingId],
                    remindDays = prefs.remindDays,
                    now = now
                )
            }
            if (fresh.isEmpty()) return@forEach

            // 알림 본문에는 현재 주의가 필요한 항목 전부를 담는다(fresh 만이 아니라).
            runCatching { showNotification(car, items) }
                .onSuccess { sent++ }
                .onFailure { android.util.Log.e(TAG, "notify failed car=${car.name}", it) }
            if (!forceTest) {
                fresh.forEach {
                    userPrefsRepository.setMaintenanceAlertNotifiedState(it.settingId, it.status.name, now)
                }
            }
        }

        // 정상으로 돌아온 항목의 기록을 지워, 다음에 다시 임박해지면 또 알리게 한다.
        // 테스트 실행은 상태를 일절 건드리지 않는다 — 정기 알림에 영향 없음.
        if (!forceTest) {
            userPrefsRepository.retainMaintenanceAlertNotifiedStates(currentIds)
        }

        // 테스트인데 보낸 게 없으면 "왜 없는지"를 알림으로 알려준다 —
        // 조용히 끝나면 기능이 고장난 것처럼 보이기 때문.
        if (forceTest && sent == 0) {
            showTestResultNotification(urgentTotal, noHistoryCount)
        }
    }

    /** 테스트 실행 전용 결과 안내 — 정기 알림 경로에서는 호출되지 않는다 */
    private fun showTestResultNotification(urgentTotal: Int, noHistoryCount: Int) {
        val reason = when {
            urgentTotal == 0 ->
                "임박·초과 상태인 정비 항목이 없어요. 아무 항목의 주기를 짧게(예: 1,000km) 바꿔 초과 상태를 만든 뒤 다시 테스트해보세요."
            noHistoryCount == urgentTotal ->
                "임박·초과 항목 ${urgentTotal}건이 전부 아직 기록이 없는 항목이라 제외됐어요. 알림은 마지막 교체 기록이 있는 항목에만 갑니다 — 기록을 하나 입력한 뒤 다시 테스트해보세요."
            else ->
                "임박·초과 ${urgentTotal}건 중 기록 없음 ${noHistoryCount}건은 제외됐고, 나머지는 임박/초과 알림 스위치 설정으로 제외됐어요."
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            AutoLogNotificationHelper.MAINT_SOON_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentTitle("테스트: 보낼 알림이 없어요")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(TEST_RESULT_NOTIFICATION_ID, notification)
    }

    private fun showNotification(car: Car, items: List<MaintenanceUiModel>) {
        val context = applicationContext
        val hasOverdue = items.any { it.status == MaintenanceStatus.OVERDUE }
        val channelId =
            if (hasOverdue) AutoLogNotificationHelper.MAINT_OVERDUE_CHANNEL_ID
            else AutoLogNotificationHelper.MAINT_SOON_CHANNEL_ID

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CAR_DETAIL
            putExtra(EXTRA_CAR_ID, car.id)
            data = Uri.parse("autolog://alert/car/${car.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (REQUEST_CODE_BASE + car.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                if (hasOverdue) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        if (items.size == 1) {
            val item = items.first()
            val action = item.name.toActionName()
            val title =
                if (item.status == MaintenanceStatus.OVERDUE) "$action 시기가 지났어요"
                else "$action${action.subjectParticle()} 다가오고 있어요"
            val body = "${car.name} · ${item.remainingText}"
            builder.setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        } else {
            val title = "${car.name} — 확인할 정비가 ${items.size}건 있어요"
            val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
            items.forEach { style.addLine("${it.name} — ${it.remainingText}") }
            builder.setContentTitle(title)
                .setContentText(items.joinToString(", ") { it.name })
                .setStyle(style)
        }

        NotificationManagerCompat.from(context)
            .notify((NOTIFICATION_ID_BASE + car.id).toInt(), builder.build())
    }

    companion object {
        /** 테스트용 inputData 키 — 디버그 전용 테스트 버튼(enqueueTest)에서만 쓰인다 */
        const val KEY_FORCE_TEST = "force_test"

        private const val TAG = "MaintAlert"

        // 주간 알림(1001)과 겹치지 않는 대역
        private const val NOTIFICATION_ID_BASE = 2000L
        private const val REQUEST_CODE_BASE = 3000L
        private const val TEST_RESULT_NOTIFICATION_ID = 2999
    }
}

/** "엔진오일" → "엔진오일 교체", 이미 교체/교환/점검/보충으로 끝나면 그대로 */
private fun String.toActionName(): String {
    val trimmed = trimEnd()
    val core = trimmed.substringBeforeLast('(').trimEnd()
    val endsWithAction = listOf("교체", "교환", "점검", "보충").any {
        trimmed.endsWith(it) || core.endsWith(it) || trimmed.endsWith("$it)")
    }
    return if (endsWithAction) trimmed else "$trimmed 교체"
}

/** 받침 유무에 따라 이/가 — "교체"→가, "점검"→이 */
private fun String.subjectParticle(): String {
    val last = lastOrNull() ?: return "가"
    if (last !in '가'..'힣') return "가"
    return if ((last - '가') % 28 != 0) "이" else "가"
}
