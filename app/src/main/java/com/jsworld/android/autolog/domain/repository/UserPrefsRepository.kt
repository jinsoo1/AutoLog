package com.jsworld.android.autolog.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPrefsRepository {
    fun observeAutoMileageUpdate(carId: Long): Flow<Boolean>
    suspend fun setAutoMileageUpdate(carId: Long, enabled: Boolean)
    fun observeWeeklyMileageNotificationEnabled(): Flow<Boolean>
    suspend fun setWeeklyMileageNotificationEnabled(enabled: Boolean)

    /** 마지막으로 백업에 성공한 시각(ms). 없으면 0 */
    fun observeLastBackupAt(): Flow<Long>
    suspend fun setLastBackupAt(millis: Long)

    /** 백업 리마인더 배너를 마지막으로 닫은 시각(ms). 없으면 0 */
    fun observeBackupBannerDismissedAt(): Flow<Long>
    suspend fun setBackupBannerDismissedAt(millis: Long)
}
