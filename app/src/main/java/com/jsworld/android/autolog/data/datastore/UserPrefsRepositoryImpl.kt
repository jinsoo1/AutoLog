package com.jsworld.android.autolog.data.datastore

import com.jsworld.android.autolog.domain.repository.UserPrefsRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
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
}