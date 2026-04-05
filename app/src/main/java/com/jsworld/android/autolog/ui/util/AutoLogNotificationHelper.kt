package com.jsworld.android.autolog.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AutoLogNotificationHelper {

    const val WEEKLY_MILEAGE_CHANNEL_ID = "weekly_mileage_channel"
    private const val WEEKLY_MILEAGE_CHANNEL_NAME = "주간 주행거리 알림"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WEEKLY_MILEAGE_CHANNEL_ID,
                WEEKLY_MILEAGE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "주 1회 차량 주행거리 업데이트를 알려줍니다."
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}