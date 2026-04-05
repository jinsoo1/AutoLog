package com.jsworld.android.autolog.ui.util

import android.content.Context
import com.jsworld.android.autolog.ui.data.room.repository.UserPrefsRepository
import com.jsworld.android.autolog.ui.scheduler.WeeklyMileageWorkScheduler
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WeeklyNotificationStartupManager @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {
    suspend fun restoreIfNeeded(context: Context) {
        val enabled = userPrefsRepository
            .observeWeeklyMileageNotificationEnabled()
            .first()

        if (enabled) {
            AutoLogNotificationHelper.createChannels(context)
            WeeklyMileageWorkScheduler.enqueueNext(context)
        }
    }
}