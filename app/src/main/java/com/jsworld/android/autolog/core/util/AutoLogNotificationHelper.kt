package com.jsworld.android.autolog.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object AutoLogNotificationHelper {

    const val WEEKLY_MILEAGE_CHANNEL_ID = "weekly_mileage_channel"
    private const val WEEKLY_MILEAGE_CHANNEL_NAME = "주간 주행거리 알림"

    // 임박/초과를 채널로 분리해 사용자가 시스템 설정에서도 따로 제어할 수 있게 한다.
    const val MAINT_SOON_CHANNEL_ID = "maintenance_soon_channel"
    const val MAINT_OVERDUE_CHANNEL_ID = "maintenance_overdue_channel"

    const val MONTHLY_REPORT_CHANNEL_ID = "monthly_report_channel"

    fun createChannels(context: Context) {
        val weekly = NotificationChannel(
            WEEKLY_MILEAGE_CHANNEL_ID,
            WEEKLY_MILEAGE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "주 1회 차량 주행거리 업데이트를 알려줍니다."
        }

        val maintSoon = NotificationChannel(
            MAINT_SOON_CHANNEL_ID,
            "교체 임박 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "정비 항목의 교체 시기가 다가오면 알려줍니다."
        }

        val maintOverdue = NotificationChannel(
            MAINT_OVERDUE_CHANNEL_ID,
            "교체 시기 초과 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "정비 항목의 교체 시기를 넘기면 알려줍니다."
        }

        val monthlyReport = NotificationChannel(
            MONTHLY_REPORT_CHANNEL_ID,
            "월간 리포트 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "매월 1일 지난달 지출 리포트가 준비되면 알려줍니다."
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(weekly, maintSoon, maintOverdue, monthlyReport))
    }

    /** 알림이 시스템에서 막혀 있는 상태 — 앱은 켜져 있는데 알림이 오지 않는 원인 */
    enum class NotificationBlock { NONE, APP_DISABLED, CHANNEL_BLOCKED }

    /**
     * 앱 설정에서 알림을 켰는데도 시스템이 막고 있는지 확인한다.
     *
     * 두 층을 모두 봐야 한다 — 앱 전체 권한(Android 13+ 거부)과 **채널별 차단**.
     * 채널만 끈 경우 areNotificationsEnabled() 는 true 라서, 이걸 놓치면
     * "앱에서 켰는데 알림이 안 온다"는 문의를 설명할 수 없다.
     */
    fun checkBlocked(context: Context, channelIds: List<String>): NotificationBlock {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return NotificationBlock.APP_DISABLED
        }

        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return NotificationBlock.NONE

        // 아직 만들지 않은 채널(알림을 한 번도 켜지 않은 상태)은 차단이 아니다.
        val blocked = channelIds.mapNotNull { manager.getNotificationChannel(it) }
            .any { it.importance == NotificationManager.IMPORTANCE_NONE }

        return if (blocked) NotificationBlock.CHANNEL_BLOCKED else NotificationBlock.NONE
    }

    /** 시스템 알림 설정 화면 — 채널 차단이면 그 채널 화면으로 바로 보낸다 */
    fun notificationSettingsIntent(context: Context, channelId: String? = null): Intent =
        if (channelId != null) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
}
