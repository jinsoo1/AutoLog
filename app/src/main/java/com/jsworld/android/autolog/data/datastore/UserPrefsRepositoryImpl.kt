package com.jsworld.android.autolog.data.datastore

import com.jsworld.android.autolog.domain.model.MaintenanceAlertNotifiedState
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class UserPrefsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPrefsRepository {

    /**
     * 기존 차량별 자동 주행거리 업데이트 설정
     */
    private fun autoMileageKey(carId: Long) =
        booleanPreferencesKey("auto_mileage_update_$carId")

    override fun observeAutoMileageUpdate(carId: Long): Flow<Boolean> =
        dataStore.data.map { it[autoMileageKey(carId)] ?: false }

    override suspend fun setAutoMileageUpdate(carId: Long, enabled: Boolean) {
        dataStore.edit { it[autoMileageKey(carId)] = enabled }
    }



    /**
     * 새 앱 전체 주간 알림 설정
     */
    private val weeklyMileageNotificationEnabledKey =
        booleanPreferencesKey("weekly_mileage_notification_enabled")

    override fun observeWeeklyMileageNotificationEnabled(): Flow<Boolean> =
        dataStore.data.map { it[weeklyMileageNotificationEnabledKey] ?: false }

    override suspend fun setWeeklyMileageNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[weeklyMileageNotificationEnabledKey] = enabled }
    }


    /**
     * 백업 리마인더용 시각 기록
     */
    private val lastBackupAtKey = longPreferencesKey("last_backup_at")

    override fun observeLastBackupAt(): Flow<Long> =
        dataStore.data.map { it[lastBackupAtKey] ?: 0L }

    override suspend fun setLastBackupAt(millis: Long) {
        dataStore.edit { it[lastBackupAtKey] = millis }
    }

    /**
     * 탭 화면의 현재 차량
     */
    private val selectedCarIdKey = longPreferencesKey("selected_car_id")

    override fun observeSelectedCarId(): Flow<Long?> =
        dataStore.data.map { prefs -> prefs[selectedCarIdKey]?.takeIf { it > 0L } }

    override suspend fun setSelectedCarId(carId: Long) {
        dataStore.edit { it[selectedCarIdKey] = carId }
    }


    private val backupBannerDismissedAtKey = longPreferencesKey("backup_banner_dismissed_at")

    override fun observeBackupBannerDismissedAt(): Flow<Long> =
        dataStore.data.map { it[backupBannerDismissedAtKey] ?: 0L }

    override suspend fun setBackupBannerDismissedAt(millis: Long) {
        dataStore.edit { it[backupBannerDismissedAtKey] = millis }
    }

    private val backupPromptRecordCountKey = intPreferencesKey("backup_prompt_record_count")

    override fun observeBackupPromptRecordCount(): Flow<Int> =
        dataStore.data.map { it[backupPromptRecordCountKey] ?: 0 }

    override suspend fun setBackupPromptRecordCount(count: Int) {
        dataStore.edit { it[backupPromptRecordCountKey] = count.coerceAtLeast(0) }
    }

    /**
     * 월간 리포트 도착 알림 — 기본 켜짐 (인터페이스 주석 참조)
     */
    private val monthlyReportEnabledKey =
        booleanPreferencesKey("monthly_report_notification_enabled")

    override fun observeMonthlyReportNotificationEnabled(): Flow<Boolean> =
        dataStore.data.map { it[monthlyReportEnabledKey] ?: true }

    override suspend fun setMonthlyReportNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[monthlyReportEnabledKey] = enabled }
    }

    /**
     * 정비 임박/초과 푸시 알림 설정
     */
    private val alertEnabledKey = booleanPreferencesKey("maintenance_alert_enabled")
    private val alertSoonKey = booleanPreferencesKey("maintenance_alert_soon")
    private val alertOverdueKey = booleanPreferencesKey("maintenance_alert_overdue")
    private val alertHourKey = intPreferencesKey("maintenance_alert_hour")
    private val alertRemindDaysKey = intPreferencesKey("maintenance_alert_remind_days")

    override fun observeMaintenanceAlertPrefs(): Flow<MaintenanceAlertPrefs> =
        dataStore.data.map { prefs ->
            MaintenanceAlertPrefs(
                enabled = prefs[alertEnabledKey] ?: false,
                soonEnabled = prefs[alertSoonKey] ?: true,
                overdueEnabled = prefs[alertOverdueKey] ?: true,
                hour = prefs[alertHourKey] ?: MaintenanceAlertPrefs.DEFAULT_HOUR,
                remindDays = prefs[alertRemindDaysKey] ?: 0
            )
        }

    override suspend fun setMaintenanceAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[alertEnabledKey] = enabled }
    }

    override suspend fun setMaintenanceAlertSoonEnabled(enabled: Boolean) {
        dataStore.edit { it[alertSoonKey] = enabled }
    }

    override suspend fun setMaintenanceAlertOverdueEnabled(enabled: Boolean) {
        dataStore.edit { it[alertOverdueKey] = enabled }
    }

    override suspend fun setMaintenanceAlertHour(hour: Int) {
        dataStore.edit { it[alertHourKey] = hour.coerceIn(0, 23) }
    }

    override suspend fun setMaintenanceAlertRemindDays(days: Int) {
        dataStore.edit { it[alertRemindDaysKey] = days.coerceAtLeast(0) }
    }

    /**
     * 항목별 마지막 알림 상태 — "maintenance_alert_state_<settingId>" = "STATUS|millis".
     * 항목 수가 수십 개 수준이라 DataStore 로 충분하다(테이블 추가·마이그레이션 회피).
     */
    private val monthlyReportPermissionAskedKey =
        booleanPreferencesKey("monthly_report_permission_asked")

    override fun observeMonthlyReportPermissionAsked(): Flow<Boolean> =
        dataStore.data.map { it[monthlyReportPermissionAskedKey] ?: false }

    override suspend fun setMonthlyReportPermissionAsked() {
        dataStore.edit { it[monthlyReportPermissionAskedKey] = true }
    }

    private val scheduleAlertEnabledKey = booleanPreferencesKey("schedule_alert_enabled")

    override fun observeScheduleAlertEnabled(): Flow<Boolean> =
        dataStore.data.map { it[scheduleAlertEnabledKey] ?: true }

    override suspend fun setScheduleAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[scheduleAlertEnabledKey] = enabled }
    }

    private fun scheduleStageKey(scheduleId: Long) =
        stringPreferencesKey("$SCHEDULE_STAGE_PREFIX$scheduleId")

    override suspend fun getScheduleAlertStages(): Map<Long, String> {
        val prefs = dataStore.data.first()
        return prefs.asMap().mapNotNull { (key, value) ->
            val id = key.name.removePrefix(SCHEDULE_STAGE_PREFIX)
                .takeIf { it != key.name }?.toLongOrNull() ?: return@mapNotNull null
            val stage = value as? String ?: return@mapNotNull null
            id to stage
        }.toMap()
    }

    override suspend fun setScheduleAlertStage(scheduleId: Long, stage: String) {
        dataStore.edit { it[scheduleStageKey(scheduleId)] = stage }
    }

    override suspend fun retainScheduleAlertStages(keep: Set<Long>) {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { key ->
                    val id = key.name.removePrefix(SCHEDULE_STAGE_PREFIX)
                        .takeIf { it != key.name }?.toLongOrNull()
                    id != null && id !in keep
                }
                .forEach { prefs -= it }
        }
    }

    private fun alertStateKey(settingId: Long) =
        stringPreferencesKey("$ALERT_STATE_PREFIX$settingId")

    override suspend fun getMaintenanceAlertNotifiedStates(): Map<Long, MaintenanceAlertNotifiedState> {
        val prefs = dataStore.data.first()
        return prefs.asMap().mapNotNull { (key, value) ->
            val id = key.name.removePrefix(ALERT_STATE_PREFIX)
                .takeIf { it != key.name }?.toLongOrNull() ?: return@mapNotNull null
            val parts = (value as? String)?.split('|') ?: return@mapNotNull null
            val status = parts.getOrNull(0) ?: return@mapNotNull null
            val at = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            id to MaintenanceAlertNotifiedState(status, at)
        }.toMap()
    }

    override suspend fun setMaintenanceAlertNotifiedState(
        settingId: Long,
        status: String,
        notifiedAt: Long
    ) {
        dataStore.edit { it[alertStateKey(settingId)] = "$status|$notifiedAt" }
    }

    override suspend fun retainMaintenanceAlertNotifiedStates(keep: Set<Long>) {
        dataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { key ->
                    val id = key.name.removePrefix(ALERT_STATE_PREFIX)
                        .takeIf { it != key.name }?.toLongOrNull()
                    id != null && id !in keep
                }
                .forEach { prefs -= it }
        }
    }

    private companion object {
        const val SCHEDULE_STAGE_PREFIX = "schedule_alert_stage_"
        const val ALERT_STATE_PREFIX = "maintenance_alert_state_"
    }
}