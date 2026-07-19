package com.jsworld.android.autolog.presentation.state

sealed class ExcelExportUiState {
    data object Idle : ExcelExportUiState()
    data object Loading : ExcelExportUiState()
    data object Success : ExcelExportUiState()

    data class Error(
        val message: String,
        val detail: String? = null
    ) : ExcelExportUiState()
}