package com.jsworld.android.autolog.presentation.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.domain.model.seasonKey
import com.jsworld.android.autolog.domain.model.seasonalGuide
import com.jsworld.android.autolog.domain.model.seasonalNotificationBody
import com.jsworld.android.autolog.domain.model.shouldNotifySeason
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.activity.MainActivity
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 계절별 관리 알림 — 홈 카드와 **같은 내용을 계절마다 딱 한 번** 보낸다.
 *
 * 정비 알림처럼 상태가 바뀔 때 울리는 게 아니라, 계절이라는 바깥 사정으로 울린다.
 * 그래서 "이 계절에 이미 보냈나"만 보면 되고 판정도 [shouldNotifySeason] 한 줄이다.
 *
 * 날짜 일정과 같은 하루 1회 체인에 얹는다 — 워커를 늘리면 끊길 곳도 늘어난다.
 */
@Singleton
class SeasonalCareNotifier @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {

    /** @return 계절 알림이 켜져 있는지 — 내일 체인을 이어야 하는지 판단에 쓴다 */
    suspend fun checkAndNotify(
        context: Context,
        today: LocalDate = LocalDate.now(),
        forceTest: Boolean = false
    ): Boolean {
        val enabled = runCatching { userPrefsRepository.observeSeasonalCareAlertEnabled().first() }
            .getOrDefault(true)
        if (!enabled) return false

        val currentKey = seasonKey(today)
        if (!forceTest) {
            val notified = runCatching { userPrefsRepository.getSeasonalCareNotifiedKey() }
                .getOrDefault("")
            val dismissed = runCatching {
                userPrefsRepository.observeSeasonalCareDismissedKey().first()
            }.getOrDefault("")
            if (!shouldNotifySeason(currentKey, notified, dismissed)) return true
        }

        // ⚠️ 권한이 없으면 보내지 못한다. 이때 '보냈다'고 기록해두면 권한을 허용한
        // 뒤에도 이번 계절은 영영 오지 않는다 — 기록하지 않고 다음 날 다시 시도한다.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return true

        val guide = seasonalGuide(today)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = seasonalNotificationBody(guide)
        val notification = NotificationCompat.Builder(
            context,
            AutoLogNotificationHelper.SEASONAL_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentTitle(guide.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\n${guide.subtitle}"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

        if (!forceTest) {
            runCatching { userPrefsRepository.setSeasonalCareNotifiedKey(currentKey) }
        }
        return true
    }

    private companion object {
        // 주간(1001)·정비(2000+)·리포트(4000)·일정(5000+) 대역과 겹치지 않게
        const val NOTIFICATION_ID = 6000
        const val REQUEST_CODE = 6100
    }
}
