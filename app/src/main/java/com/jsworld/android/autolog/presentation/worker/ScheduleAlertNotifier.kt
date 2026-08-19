package com.jsworld.android.autolog.presentation.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.core.util.Constant.ACTION_OPEN_SCHEDULE
import com.jsworld.android.autolog.core.util.Constant.EXTRA_CAR_ID
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.model.CarSchedule
import com.jsworld.android.autolog.domain.model.ScheduleAlertStage
import com.jsworld.android.autolog.domain.model.formatScheduleDate
import com.jsworld.android.autolog.domain.model.scheduleAlertStage
import com.jsworld.android.autolog.domain.model.scheduleAlertText
import com.jsworld.android.autolog.domain.model.shouldNotifySchedule
import com.jsworld.android.autolog.domain.repository.CarScheduleRepository
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.activity.MainActivity
import jakarta.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * 날짜 일정 알림 — 하루 1회 도는 정비 알림 워커에 얹어 검사한다.
 *
 * 새 워커를 만들지 않는 이유: 같은 주기(하루 1회)에 같은 성격의 검사라
 * 체인이 하나 더 늘면 그만큼 끊길 자리도 늘어난다(1.2.2에서 겪은 문제).
 *
 * 스팸 방지는 정비 알림과 같은 원칙 — **단계가 넘어갈 때만** 보낸다
 * (2주 전 → 1주 전 → 당일 → 지남).
 */
class ScheduleAlertNotifier @Inject constructor(
    private val scheduleRepository: CarScheduleRepository,
    private val userPrefsRepository: UserPrefsRepository
) {

    /**
     * @param carsById 알림 제목에 차량 이름을 넣기 위한 조회용
     * @param forceTest 디버그 테스트 — 전이 검사 없이 임박한 일정을 전부 보낸다
     */
    suspend fun checkAndNotify(
        context: Context,
        carsById: Map<Long, Car>,
        today: LocalDate = LocalDate.now(),
        forceTest: Boolean = false
    ): Int {
        // 설정 읽기가 실패해도 알림 자체를 막지는 않는다(기본 켜짐)
        val enabled = runCatching {
            userPrefsRepository.observeScheduleAlertEnabled().first()
        }.getOrDefault(true)
        if (!enabled) return 0

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return 0

        val schedules = runCatching { scheduleRepository.getAll() }.getOrDefault(emptyList())
        if (schedules.isEmpty()) return 0

        val previous = runCatching { userPrefsRepository.getScheduleAlertStages() }
            .getOrDefault(emptyMap())

        var sent = 0
        schedules.forEach { schedule ->
            val remaining = schedule.remainingDays(today) ?: return@forEach
            val stage = scheduleAlertStage(remaining)
            val prev = previous[schedule.id]?.let { name ->
                runCatching { ScheduleAlertStage.valueOf(name) }.getOrNull()
            }

            // 테스트 실행은 단계를 따지지 않는다 — 확인하려는 건 "알림이 제대로 그려지는가"이고,
            // 임박한 일정이 없다고 조용히 끝나면 고장난 것처럼 보인다(정비 테스트와 같은 취지).
            val notify = if (forceTest) true else shouldNotifySchedule(stage, prev)
            if (!notify) return@forEach

            runCatching {
                notify(context, schedule, carsById[schedule.carId], remaining)
            }.onSuccess { sent++ }

            if (!forceTest) {
                userPrefsRepository.setScheduleAlertStage(schedule.id, stage.name)
            }
        }

        // 삭제됐거나 다음 회차로 넘어간 일정의 기록은 정리한다 —
        // 완료로 날짜가 밀리면 단계도 처음(FAR)부터 다시 시작해야 한다.
        if (!forceTest) {
            val stillFar = schedules
                .filter { s ->
                    val r = s.remainingDays(today)
                    r != null && scheduleAlertStage(r) != ScheduleAlertStage.FAR
                }
                .map { it.id }
                .toSet()
            runCatching { userPrefsRepository.retainScheduleAlertStages(stillFar) }
        }

        return sent
    }

    private fun notify(
        context: Context,
        schedule: CarSchedule,
        car: Car?,
        remainingDays: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_SCHEDULE
            putExtra(EXTRA_CAR_ID, schedule.carId)
            data = Uri.parse("autolog://schedule/${schedule.carId}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (REQUEST_CODE_BASE + schedule.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = scheduleAlertText(schedule.title, remainingDays)
        val body = listOfNotNull(
            car?.name,
            formatScheduleDate(schedule.dueDate, LocalDate.now()),
            schedule.memo?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        val notification = NotificationCompat.Builder(
            context,
            AutoLogNotificationHelper.SCHEDULE_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context)
            .notify((NOTIFICATION_ID_BASE + schedule.id).toInt(), notification)
    }

    private companion object {
        // 주간(1001)·정비(2000+)·리포트(4000) 대역과 겹치지 않게
        const val NOTIFICATION_ID_BASE = 5000L
        const val REQUEST_CODE_BASE = 5100L
    }
}
