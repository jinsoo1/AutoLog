package com.jsworld.android.autolog.domain.repository

import com.jsworld.android.autolog.domain.model.MaintenanceAlertNotifiedState
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
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

    /** 정비 임박/초과 푸시 알림 설정 */
    fun observeMaintenanceAlertPrefs(): Flow<MaintenanceAlertPrefs>
    suspend fun setMaintenanceAlertEnabled(enabled: Boolean)
    suspend fun setMaintenanceAlertSoonEnabled(enabled: Boolean)
    suspend fun setMaintenanceAlertOverdueEnabled(enabled: Boolean)
    suspend fun setMaintenanceAlertHour(hour: Int)
    suspend fun setMaintenanceAlertRemindDays(days: Int)

    /** settingId → 마지막으로 알림 보낸 상태. 전이 감지용 */
    suspend fun getMaintenanceAlertNotifiedStates(): Map<Long, MaintenanceAlertNotifiedState>
    suspend fun setMaintenanceAlertNotifiedState(settingId: Long, status: String, notifiedAt: Long)
    /** keep 에 없는 항목의 기록을 지운다 — 정상으로 돌아온 항목이 다음에 다시 알림받게 */
    suspend fun retainMaintenanceAlertNotifiedStates(keep: Set<Long>)
}
