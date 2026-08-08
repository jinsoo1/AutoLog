package com.jsworld.android.autolog.presentation.state

import com.jsworld.android.autolog.data.repository.BackupFileInfo
import com.jsworld.android.autolog.data.repository.BackupPreview

/** 복원 확인 다이얼로그에서 보여줄 백업 요약 상태 */
data class RestorePreviewUiState(
    val loading: Boolean = false,
    val preview: BackupPreview? = null,
    val error: String? = null
)

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
