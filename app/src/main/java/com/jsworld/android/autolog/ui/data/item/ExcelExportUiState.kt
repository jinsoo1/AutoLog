package com.jsworld.android.autolog.ui.data.item

sealed class ExcelExportUiState {
    data object Idle : ExcelExportUiState()
    data object Loading : ExcelExportUiState()
    data object Success : ExcelExportUiState()
    data class Error(val message: String) : ExcelExportUiState()
}