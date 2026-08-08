package com.jsworld.android.autolog.presentation.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jsworld.android.autolog.R
import com.jsworld.android.autolog.domain.model.Car
import com.jsworld.android.autolog.domain.repository.CarRepository
import com.jsworld.android.autolog.presentation.scheduler.WeeklyMileageWorkScheduler
import com.jsworld.android.autolog.core.util.AutoLogNotificationHelper
import com.jsworld.android.autolog.core.util.WeekTimeUtils
import com.jsworld.android.autolog.presentation.activity.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.collections.first
import kotlin.collections.take

@HiltWorker
class WeeklyMileageReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val carRepository: CarRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        android.util.Log.d("WeeklyWorker", "doWork start")

        return try {
            val weekStartMillis = WeekTimeUtils.getStartOfWeekMillis()
            val targetCars = carRepository.getCarsNeedingWeeklyMileageUpdate(weekStartMillis)

            android.util.Log.d("WeeklyWorker", "targetCars=${targetCars.size}")

            if (targetCars.isNotEmpty()) {
                showNotification(targetCars)
                android.util.Log.d("WeeklyWorker", "notification shown")
            } else {
                android.util.Log.d("WeeklyWorker", "no cars to notify")
            }

            WeeklyMileageWorkScheduler.enqueueNext(applicationContext)
            android.util.Log.d("WeeklyWorker", "next work enqueued")

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WeeklyWorker", "doWork failed", e)

            // 다음 주 예약은 유지
            WeeklyMileageWorkScheduler.enqueueNext(applicationContext)
            Result.failure()
        }
    }

    private fun showNotification(cars: List<Car>) {
        val context = applicationContext

        val title = if (cars.size == 1) {
            "${cars.first().name} 주행거리 업데이트가 필요해요"
        } else {
            "주행거리 업데이트가 필요한 차량 ${cars.size}대가 있어요"
        }

        val content = if (cars.size == 1) {
            "이번 주 주행거리 업데이트가 아직 없어요."
        } else {
            cars.take(3).joinToString(", ") { it.name } +
                    if (cars.size > 3) " 외 ${cars.size - 3}대" else ""
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            AutoLogNotificationHelper.WEEKLY_MILEAGE_CHANNEL_ID
        )
            // 알림 아이콘은 시스템이 단색으로 틴트하므로 컬러 런처 아이콘을 쓰면 뭉개진다.
            .setSmallIcon(R.drawable.ic_stat_autolog)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}