package com.jsworld.android.autolog.presentation.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.presentation.state.BackupUiEvent
import com.jsworld.android.autolog.presentation.state.BackupUiState
import com.jsworld.android.autolog.presentation.state.RestorePreviewUiState
import com.jsworld.android.autolog.data.repository.BackupRepository
import com.jsworld.android.autolog.domain.model.MaintenanceAlertPrefs
import com.jsworld.android.autolog.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPrefsRepository: UserPrefsRepository,
    private val backupRepository: BackupRepository
) : AndroidViewModel(application) {

    val appVersion: String = run {
        val context = getApplication<Application>()

        runCatching {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                context.packageName,
                0
            )

            packageInfo.versionName ?: ""
        }.getOrDefault("")
    }

    val weeklyMileageNotificationEnabled: Flow<Boolean> =
        userPrefsRepository.observeWeeklyMileageNotificationEnabled()

    val maintenanceAlertPrefs: Flow<MaintenanceAlertPrefs> =
        userPrefsRepository.observeMaintenanceAlertPrefs()

    val monthlyReportNotificationEnabled: Flow<Boolean> =
        userPrefsRepository.observeMonthlyReportNotificationEnabled()

    fun setMonthlyReportNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setMonthlyReportNotificationEnabled(enabled)
        }
    }

    fun setMaintenanceAlertEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setMaintenanceAlertEnabled(enabled) }
    }

    fun setMaintenanceAlertSoonEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setMaintenanceAlertSoonEnabled(enabled) }
    }

    fun setMaintenanceAlertOverdueEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsRepository.setMaintenanceAlertOverdueEnabled(enabled) }
    }

    fun setMaintenanceAlertHour(hour: Int) {
        viewModelScope.launch { userPrefsRepository.setMaintenanceAlertHour(hour) }
    }

    fun setMaintenanceAlertRemindDays(days: Int) {
        viewModelScope.launch { userPrefsRepository.setMaintenanceAlertRemindDays(days) }
    }

    val lastBackupAt: Flow<Long> =
        userPrefsRepository.observeLastBackupAt()

    private val _backupUiState = MutableStateFlow(
        BackupUiState()
    )
    val backupUiState: StateFlow<BackupUiState> =
        _backupUiState.asStateFlow()

    private val _backupEvent = Channel<BackupUiEvent>(
        capacity = Channel.BUFFERED
    )
    val backupEvent: Flow<BackupUiEvent> =
        _backupEvent.receiveAsFlow()

    fun setWeeklyMileageNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository
                .setWeeklyMileageNotificationEnabled(enabled)
        }
    }

    /**
     * 백업 성공 공통 처리 — 시각과 함께 그 시점의 기록 수도 저장한다.
     * 백업 권유 다이얼로그가 이 기준점에서 +N건 쌓였을 때만 다시 권한다.
     */
    private suspend fun markBackedUp() {
        userPrefsRepository.setLastBackupAt(System.currentTimeMillis())
        runCatching {
            userPrefsRepository.setBackupPromptRecordCount(backupRepository.countAllRecords())
        }
    }

    fun exportBackup(uri: Uri) {
        if (_backupUiState.value.isExporting) return

        viewModelScope.launch {
            _backupUiState.update {
                it.copy(isExporting = true)
            }

            backupRepository.exportBackup(uri)
                .onSuccess {
                    markBackedUp()
                    _backupEvent.send(
                        BackupUiEvent.ExportSuccess()
                    )
                }
                .onFailure { throwable ->
                    _backupEvent.send(
                        BackupUiEvent.ExportFailure(
                            message = throwable.message
                                ?: "백업 파일 생성에 실패했습니다."
                        )
                    )
                }

            _backupUiState.update {
                it.copy(isExporting = false)
            }
        }
    }

    /** Download/AutoLog 폴더에 바로 저장 (위치 선택 불필요) */
    fun exportToFolder() {
        if (_backupUiState.value.isExporting) return

        viewModelScope.launch {
            _backupUiState.update { it.copy(isExporting = true) }

            backupRepository.exportToAutoLogFolder()
                .onSuccess { path ->
                    markBackedUp()
                    _backupEvent.send(BackupUiEvent.ExportSuccess(path))
                    refreshBackupsInternal()
                }
                .onFailure { throwable ->
                    _backupEvent.send(
                        BackupUiEvent.ExportFailure(
                            message = throwable.message
                                ?: "백업 파일 생성에 실패했습니다."
                        )
                    )
                }

            _backupUiState.update { it.copy(isExporting = false) }
        }
    }

    /** 복원 목록용: Download/AutoLog 폴더의 백업 파일 조회 */
    fun refreshBackups() {
        viewModelScope.launch {
            _backupUiState.update { it.copy(isLoadingBackups = true) }
            val list = runCatching { backupRepository.listAutoLogBackups() }
                .getOrDefault(emptyList())
            _backupUiState.update { it.copy(backups = list, isLoadingBackups = false) }
        }
    }

    private suspend fun refreshBackupsInternal() {
        val list = runCatching { backupRepository.listAutoLogBackups() }
            .getOrDefault(emptyList())
        _backupUiState.update { it.copy(backups = list) }
    }

    /**
     * 복원 확인 다이얼로그에 보여줄 백업 요약. 파일을 읽기만 하고 적용하지 않는다.
     */
    private val _restorePreview = MutableStateFlow<RestorePreviewUiState?>(null)
    val restorePreview: StateFlow<RestorePreviewUiState?> = _restorePreview

    fun loadRestorePreview(uri: Uri) {
        _restorePreview.value = RestorePreviewUiState(loading = true)
        viewModelScope.launch {
            backupRepository.peekBackup(uri)
                .onSuccess { preview ->
                    _restorePreview.value = RestorePreviewUiState(
                        loading = false,
                        preview = preview
                    )
                }
                .onFailure { throwable ->
                    _restorePreview.value = RestorePreviewUiState(
                        loading = false,
                        // 여기서 실패하면 복원도 실패할 파일이다. 미리 알려준다.
                        error = throwable.message ?: "백업 파일을 읽을 수 없습니다."
                    )
                }
        }
    }

    fun clearRestorePreview() {
        _restorePreview.value = null
    }

    fun restoreBackup(
        uri: Uri
    ) {

        if (_backupUiState.value.isRestoring) return

        viewModelScope.launch {

            _backupUiState.update {

                it.copy(
                    isRestoring = true
                )
            }

            backupRepository.restoreBackup(uri)
                .onSuccess {

                    _backupEvent.send(
                        BackupUiEvent.RestoreSuccess
                    )
                }
                .onFailure {

                    _backupEvent.send(
                        BackupUiEvent.RestoreFailure(
                            it.message
                                ?: "복원에 실패했습니다."
                        )
                    )
                }

            _backupUiState.update {

                it.copy(
                    isRestoring = false
                )
            }
        }
    }
}