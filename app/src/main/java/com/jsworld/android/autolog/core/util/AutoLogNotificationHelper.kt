package com.jsworld.android.autolog.core.util

import com.jsworld.android.autolog.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object AutoLogNotificationHelper {

    const val WEEKLY_MILEAGE_CHANNEL_ID = "weekly_mileage_channel"

    // 임박/초과를 채널로 분리해 사용자가 시스템 설정에서도 따로 제어할 수 있게 한다.
    const val MAINT_SOON_CHANNEL_ID = "maintenance_soon_channel"
    const val MAINT_OVERDUE_CHANNEL_ID = "maintenance_overdue_channel"

    const val MONTHLY_REPORT_CHANNEL_ID = "monthly_report_channel"
    const val SCHEDULE_CHANNEL_ID = "schedule_channel"
    const val SEASONAL_CHANNEL_ID = "seasonal_channel"

    fun createChannels(context: Context) {
        val weekly = NotificationChannel(
            WEEKLY_MILEAGE_CHANNEL_ID,
            context.getString(R.string.channel_weekly_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_weekly_desc)
        }

        val maintSoon = NotificationChannel(
            MAINT_SOON_CHANNEL_ID,
            context.getString(R.string.channel_soon_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_soon_desc)
        }

        val maintOverdue = NotificationChannel(
            MAINT_OVERDUE_CHANNEL_ID,
            context.getString(R.string.channel_overdue_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_overdue_desc)
        }

        val monthlyReport = NotificationChannel(
            MONTHLY_REPORT_CHANNEL_ID,
            context.getString(R.string.channel_monthly_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_monthly_desc)
        }

        val schedule = NotificationChannel(
            SCHEDULE_CHANNEL_ID,
            context.getString(R.string.channel_schedule_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_schedule_desc)
        }

        val seasonal = NotificationChannel(
            SEASONAL_CHANNEL_ID,
            context.getString(R.string.channel_seasonal_name),
            // 읽는 콘텐츠라 소리로 재촉하지 않는다. 계절마다 1회뿐이기도 하다.
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_seasonal_desc)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(weekly, maintSoon, maintOverdue, monthlyReport, schedule, seasonal)
        )
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
