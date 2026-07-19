package com.jsworld.android.autolog.core.util

import android.content.Context
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import com.jsworld.android.autolog.presentation.scheduler.WeeklyMileageWorkScheduler
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