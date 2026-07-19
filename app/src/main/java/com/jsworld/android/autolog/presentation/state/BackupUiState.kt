package com.jsworld.android.autolog.presentation.state


data class BackupUiState(
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false
)

sealed interface BackupUiEvent {

    data object ExportSuccess : BackupUiEvent

    data object RestoreSuccess : BackupUiEvent

    data class ExportFailure(
        val message: String
    ) : BackupUiEvent

    data class RestoreFailure(
        val message: String
    ) : BackupUiEvent
}
