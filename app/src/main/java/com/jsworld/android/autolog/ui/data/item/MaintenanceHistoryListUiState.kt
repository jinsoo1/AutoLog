package com.jsworld.android.autolog.ui.data.item

data class MaintenanceHistoryListUiState(
    val loading: Boolean = true,
    val typeName: String = "정비 내역",
    val histories: List<MaintenanceHistory> = emptyList()
)
