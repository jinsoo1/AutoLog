package com.jsworld.android.autolog.ui.view.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jsworld.android.autolog.ui.data.item.BackupUiEvent
import com.jsworld.android.autolog.ui.data.item.BackupUiState
import com.jsworld.android.autolog.ui.data.room.repository.BackupRepository
import com.jsworld.android.autolog.ui.data.room.repository.UserPrefsRepository
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
                    _backupEvent.send(
                        BackupUiEvent.ExportSuccess
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