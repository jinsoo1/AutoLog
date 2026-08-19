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
     * 백업 권유 다이얼로그를 마지막으로 보여줬을 때의 기록 수. 0 = 보여준 적 없음.
     * '나중에'를 눌러도 기록이 이 값보다 한참 더 쌓이면 한 번 더 권한다 —
     * 잃으면 아까운 양이 커졌는데 영구히 침묵하는 건 취지와 어긋난다.
     */
    fun observeBackupPromptRecordCount(): Flow<Int>
    suspend fun setBackupPromptRecordCount(count: Int)

    /**
     * 탭 화면이 현재 보고 있는 차량. 없으면 null(→ 대표 차량으로 대체).
     * 차량이 삭제돼 유효하지 않을 수 있으므로 화면에서 실제 차량 목록과 대조해야 한다.
     */
    fun observeSelectedCarId(): Flow<Long?>
    suspend fun setSelectedCarId(carId: Long)

    /**
     * 월간 리포트 도착 알림 — **기본 켜짐**. 이 알림의 존재 이유가 리포트를 만든 걸
     * 모르는 사용자를 데려오는 것이라, 꺼두면 목적을 잃는다. 시스템 알림 권한이
     * 없으면 어차피 나가지 않고(워커에서 확인), 설정에서 끌 수 있다.
     */
    fun observeMonthlyReportNotificationEnabled(): Flow<Boolean>
    suspend fun setMonthlyReportNotificationEnabled(enabled: Boolean)

    /** 정비 임박/초과 푸시 알림 설정 */
    fun observeMaintenanceAlertPrefs(): Flow<MaintenanceAlertPrefs>
    suspend fun setMaintenanceAlertEnabled(enabled: Boolean)
    suspend fun setMaintenanceAlertSoonEnabled(enabled: Boolean)
    suspend fun setMaintenanceAlertOverdueEnabled(enabled: Boolean)
    suspend fun setMaintenanceAlertHour(hour: Int)
    suspend fun setMaintenanceAlertRemindDays(days: Int)

    /**
     * 날짜 일정 알림 — **기본 켜짐**. 일정을 직접 등록한 것 자체가 "알려달라"는
     * 신호라, 등록해두고 알림이 없으면 기능의 의미가 사라진다.
     */
    fun observeScheduleAlertEnabled(): Flow<Boolean>
    suspend fun setScheduleAlertEnabled(enabled: Boolean)

    /** scheduleId → 마지막으로 알린 단계(ScheduleAlertStage.name). 전이 감지용 */
    suspend fun getScheduleAlertStages(): Map<Long, String>
    suspend fun setScheduleAlertStage(scheduleId: Long, stage: String)
    /** keep 에 없는 일정의 기록을 지운다 — 삭제된 일정의 찌꺼기 방지 */
    suspend fun retainScheduleAlertStages(keep: Set<Long>)

    /** settingId → 마지막으로 알림 보낸 상태. 전이 감지용 */
    suspend fun getMaintenanceAlertNotifiedStates(): Map<Long, MaintenanceAlertNotifiedState>
    suspend fun setMaintenanceAlertNotifiedState(settingId: Long, status: String, notifiedAt: Long)
    /** keep 에 없는 항목의 기록을 지운다 — 정상으로 돌아온 항목이 다음에 다시 알림받게 */
    suspend fun retainMaintenanceAlertNotifiedStates(keep: Set<Long>)
}
