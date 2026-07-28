package com.jsworld.android.autolog.presentation.state

import com.jsworld.android.autolog.data.repository.BackupFileInfo


data class BackupUiState(
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val isLoadingBackups: Boolean = false,
    val backups: List<BackupFileInfo> = emptyList()
)

sealed interface BackupUiEvent {

    data class ExportSuccess(val location: String? = null) : BackupUiEvent

    data object RestoreSuccess : BackupUiEvent

    data class ExportFailure(
        val message: String
    ) : BackupUiEvent

    data class RestoreFailure(
        val message: String
    ) : BackupUiEvent
}
