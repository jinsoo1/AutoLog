package com.jsworld.android.autolog.presentation.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.presentation.state.BackupUiEvent
import com.jsworld.android.autolog.presentation.state.BackupUiState
import com.jsworld.android.autolog.data.repository.BackupRepository
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

    fun exportBackup(uri: Uri) {
        if (_backupUiState.value.isExporting) return

        viewModelScope.launch {
            _backupUiState.update {
                it.copy(isExporting = true)
            }

            backupRepository.exportBackup(uri)
                .onSuccess {
                    userPrefsRepository.setLastBackupAt(System.currentTimeMillis())
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
                    userPrefsRepository.setLastBackupAt(System.currentTimeMillis())
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