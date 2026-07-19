package com.jsworld.android.autolog.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPrefsRepository {
    fun observeAutoMileageUpdate(carId: Long): Flow<Boolean>
    suspend fun setAutoMileageUpdate(carId: Long, enabled: Boolean)
    fun observeWeeklyMileageNotificationEnabled(): Flow<Boolean>
    suspend fun setWeeklyMileageNotificationEnabled(enabled: Boolean)
}
