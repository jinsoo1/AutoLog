package com.jsworld.android.autolog.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AutoLogNotificationHelper {

    const val WEEKLY_MILEAGE_CHANNEL_ID = "weekly_mileage_channel"
    private const val WEEKLY_MILEAGE_CHANNEL_NAME = "주간 주행거리 알림"

    // 임박/초과를 채널로 분리해 사용자가 시스템 설정에서도 따로 제어할 수 있게 한다.
    const val MAINT_SOON_CHANNEL_ID = "maintenance_soon_channel"
    const val MAINT_OVERDUE_CHANNEL_ID = "maintenance_overdue_channel"

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

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(weekly, maintSoon, maintOverdue))
    }
}