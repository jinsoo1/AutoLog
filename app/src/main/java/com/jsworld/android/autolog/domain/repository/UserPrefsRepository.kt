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

    /**
     * 탭 화면이 현재 보고 있는 차량. 없으면 null(→ 대표 차량으로 대체).
     * 차량이 삭제돼 유효하지 않을 수 있으므로 화면에서 실제 차량 목록과 대조해야 한다.
     */
    fun observeSelectedCarId(): Flow<Long?>
    suspend fun setSelectedCarId(carId: Long)
}
